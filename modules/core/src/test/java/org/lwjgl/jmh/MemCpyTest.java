/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.jmh;

import org.lwjgl.system.MemoryAccess;
import org.lwjgl.system.MemoryUtil;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteOrder;

import static org.lwjgl.system.MemoryUtil.*;

@State(Scope.Benchmark)
public class MemCpyTest {

	private long f = nmemAlloc(1024);
	private long t = nmemAlloc(1024);

	@Param({ "8", "12", "16", "32", "64", "96", "128", "256", "384", "512", "768", "1024" })
	public int length;

	@Benchmark
	public void memCopy() {
		MemoryUtil.memCopy(f, t, length);
	}

	@Benchmark
	public void loop() {
		memCopyAligned(f, t, length);
	}

	@Benchmark
	public void jni() {
		MemoryAccess.memcpy(t, f, length);
	}

	private static void memCopyAligned(long src, long dst, int bytes) {
		int i = 0;

		// Aligned longs for performance
		for ( ; i <= bytes - 8; i += 8 )
			memPutLong(dst + i, memGetLong(src + i));

		// Aligned tail
		if ( i < bytes )
			memPutLong(dst + i, merge(memGetLong(dst + i), memGetLong(src + i), shl(-1L, 8 - (bytes - i))));
	}

	private static long shl(long value, int bytes) {
		if ( ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN )
			return value << (bytes << 3);
		else
			return value >>> (bytes << 3);
	}

	private static long merge(long a, long b, long mask) {
		return a ^ ((a ^ b) & mask);
	}

}
