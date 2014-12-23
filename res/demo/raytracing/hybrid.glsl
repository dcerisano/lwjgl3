/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
#version 430 core

layout(binding = 0, rgba32f) uniform image2D framebuffer;
layout(binding = 1, rgba32f) uniform readonly image2D viewPositionImage;
layout(binding = 2, rgba16f) uniform readonly image2D viewNormalImage;

uniform vec3 eye;
uniform vec3 ray00;
uniform vec3 ray01;
uniform vec3 ray10;
uniform vec3 ray11;

uniform float blendFactor;
uniform float time;
uniform int sampleCount;
uniform int bounceCount;

struct box {
  vec3 min;
  vec3 max;
  int mat;
};

#define MAX_SCENE_BOUNDS 100.0
#define NUM_BOXES 7

const box boxes[] = {
  /* The ground */
  {vec3(-5.0, -0.1, -5.0), vec3(5.0, 0.0, 5.0), 0},
  /* Box in the middle */
  {vec3(-0.5, 0.0, -0.5), vec3(0.5, 1.0, 0.5), 1},
  /* Left wall */
  {vec3(-5.1, 0.0, -5.0), vec3(-5.0, 5.0, 5.0), 2},
  /* Right wall */
  {vec3(5.0, 0.0, -5.0), vec3(5.1, 5.0, 5.0), 3},
  /* Back wall */
  {vec3(-5.0, 0.0, -5.1), vec3(5.0, 5.0, -5.0), 0},
  /* Front wall */
  {vec3(-5.0, 0.0, 5.0), vec3(5.0, 5.0, 5.1), 0},
  /* Top wall */
  {vec3(-5.0, 5.0, -5.0), vec3(5.0, 5.1, 5.0), 4}
};

#define EPSILON 0.0001
#define LIGHT_RADIUS 0.4

#define LIGHT_BASE_INTENSITY 20.0
const vec3 lightCenterPosition = vec3(1.5, 2.9, 3);
const vec4 lightColor = vec4(1);

float random(vec2 f, float time);
vec3 randomDiskPoint(vec3 rand, vec3 n, vec3 up);
vec3 randomHemispherePoint(vec3 rand, vec3 n);

struct hitinfo {
  float near;
  float far;
  int bi;
};

/*
 * We need random values every now and then.
 * So, they will be precomputed for each ray we trace and
 * can be used by any function.
 */
vec3 rand;
vec3 cameraUp;

vec2 intersectBox(vec3 origin, vec3 dir, const box b) {
  vec3 tMin = (b.min - origin) / dir;
  vec3 tMax = (b.max - origin) / dir;
  vec3 t1 = min(tMin, tMax);
  vec3 t2 = max(tMin, tMax);
  float tNear = max(max(t1.x, t1.y), t1.z);
  float tFar = min(min(t2.x, t2.y), t2.z);
  return vec2(tNear, tFar);
}

vec4 colorOfBox(const box b) {
  vec4 col;
  if (b.mat == 0) {
    col = vec4(1.0, 1.0, 1.0, 1.0);
  } else if (b.mat == 1) {
    col = vec4(1.0, 0.2, 0.2, 1.0);
  } else if (b.mat == 2) {
    col = vec4(0.2, 0.2, 1.0, 1.0);
  } else if (b.mat == 3) {
    col = vec4(0.2, 1.0, 0.2, 1.0);
  } else {
    col = vec4(0.5, 0.5, 0.5, 1.0);
  }
  return col;
}

bool intersectBoxes(vec3 origin, vec3 dir, out hitinfo info) {
  float smallest = MAX_SCENE_BOUNDS;
  bool found = false;
  for (int i = 0; i < NUM_BOXES; i++) {
    vec2 lambda = intersectBox(origin, dir, boxes[i]);
    if (lambda.x > 0.0 && lambda.x < lambda.y && lambda.x < smallest) {
      info.near = lambda.x;
      info.far = lambda.y;
      info.bi = i;
      smallest = lambda.x;
      found = true;
    }
  }
  return found;
}

vec3 normalForBox(vec3 hit, const box b) {
  if (hit.x < b.min.x + EPSILON)
    return vec3(-1.0, 0.0, 0.0);
  else if (hit.x > b.max.x - EPSILON)
    return vec3(1.0, 0.0, 0.0);
  else if (hit.y < b.min.y + EPSILON)
    return vec3(0.0, -1.0, 0.0);
  else if (hit.y > b.max.y - EPSILON)
    return vec3(0.0, 1.0, 0.0);
  else if (hit.z < b.min.z + EPSILON)
    return vec3(0.0, 0.0, -1.0);
  else
    return vec3(0.0, 0.0, 1.0);
}

vec4 trace(vec3 origin, vec3 dir, vec3 hitPoint, vec3 normal) {
  hitinfo i;
  vec4 accumulated = vec4(0.0);
  vec4 attenuation = vec4(1.0);
  box b;
  do {
    vec3 lightNormal = normalize(hitPoint - lightCenterPosition);
    vec3 lightPosition = lightCenterPosition + randomDiskPoint(rand, lightNormal, cameraUp) * LIGHT_RADIUS;
    vec3 shadowRayDir = lightPosition - hitPoint;
    vec3 shadowRayStart = hitPoint + normal * EPSILON;
    hitinfo shadowRayInfo;
    bool lightObstructed = intersectBoxes(shadowRayStart, shadowRayDir, shadowRayInfo);
    attenuation *= colorOfBox(b);
    if (shadowRayInfo.near >= 1.0) {
      float cosineFallOff = max(0.0, dot(normal, normalize(shadowRayDir)));
      float oneOverR2 = 1.0 / dot(shadowRayDir, shadowRayDir);
      accumulated += attenuation * vec4(lightColor * LIGHT_BASE_INTENSITY * cosineFallOff * oneOverR2);
    }
    origin = shadowRayStart;
    dir = randomHemispherePoint(rand, normal);
    attenuation *= dot(normal, dir);
  } while (false);
  return accumulated;
}

layout (local_size_x = 16, local_size_y = 8) in;

void main(void) {
  ivec2 pix = ivec2(gl_GlobalInvocationID.xy);
  ivec2 size = imageSize(framebuffer);
  if (pix.x >= size.x || pix.y >= size.y) {
    return;
  }

  vec3 viewPosition = imageLoad(viewPositionImage, pix).xyz;
  vec3 viewNormal = imageLoad(viewNormalImage, pix).xyz;

  vec2 pos = vec2(pix) / vec2(size.x - 1, size.y - 1);
  cameraUp = normalize(ray01 - ray00);

  float rand1 = random(pix, time);
  float rand2 = random(pix + vec2(641.51224, 423.178), time);
  float rand3 = random(pix - vec2(147.16414, 363.941), time);
  rand = vec3(rand1, rand2, rand3);

  vec2 jitter = vec2(rand1, rand2) / vec2(size);
  vec2 p = pos + jitter;
  vec3 dir = mix(mix(ray00, ray01, p.y), mix(ray10, ray11, p.y), p.x);

  vec4 color = vec4(0.0, 0.0, 0.0, 1.0);
  color += trace(eye, dir, viewPosition, viewNormal);

  vec4 oldColor = vec4(0.0);
  if (blendFactor > 0.0) {
    /* Without explicitly disabling imageLoad for
     * the first frame, we get VERY STRANGE corrupted image!
     * 'mix' SHOULD mix oldColor out, but strangely it does not!
     */
    oldColor = imageLoad(framebuffer, pix);
  }

  vec4 finalColor = mix(color, oldColor, blendFactor);
  imageStore(framebuffer, pix, finalColor);
}