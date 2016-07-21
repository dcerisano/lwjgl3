/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.jmh;

import org.lwjgl.system.MemoryAccess;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteOrder;
import java.util.Arrays;

import static org.lwjgl.system.MemoryUtil.*;

@State(Scope.Benchmark)
public class MemSetTest {

	private static long m = nmemCalloc(1, 2048) + 1;

	private static byte[] a = new byte[1024];

	@Param({ "16", "32", "64", "128", "256", "1024" })
	public int length;

	@Benchmark
	public void memset() {
		memSet(m, 0, length);
	}

	@Benchmark
	public void loop() {
		memSetLoop(m, (byte)0, length);
	}

	@Benchmark
	public void jni() {
		MemoryAccess.memset(m, 0, length);
	}

	@Benchmark
	public void arrayFill() {
		Arrays.fill(a, 0, length, (byte)0);
	}

	private static void memSetLoop(long dst, byte value, int bytes) {
		int i = 0;

		int misalignment = (int)dst & 7;
		long fill = fill(value);

		if ( 8 <= bytes ) {
			if ( misalignment != 0 ) {
				memPutLong(dst - misalignment, merge(memGetLong(dst - misalignment), fill, shr(-1L, misalignment)));
				i += 8 - misalignment;
			}

			// Aligned longs for performance
			for ( ; i <= bytes - 8; i += 8 )
				memPutLong(dst + i, fill);
		} else if ( misalignment != 0 && 0 < bytes ) {
			memPutLong(dst - misalignment, merge(memGetLong(dst - misalignment), fill, shr(shl(-1L, 8 - bytes), misalignment)));
			i += 8 - misalignment;
		}

		// Aligned tail
		if ( i < bytes )
			memPutLong(dst + i, merge(memGetLong(dst + i), fill, shl(-1L, 8 - (bytes - i))));
	}

	private static long shl(long value, int bytes) {
		if ( ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN )
			return value << (bytes << 3);
		else
			return value >>> (bytes << 3);
	}

	private static long shr(long value, int bytes) {
		if ( ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN )
			return value >>> (bytes << 3);
		else
			return value << (bytes << 3);
	}

	private static long merge(long a, long b, long mask) {
		return a ^ ((a ^ b) & mask);
	}

	private static long fill(byte value) {
		long fill = value;

		if ( value != 0 ) {
			fill |= fill << 8;
			fill |= fill << 16;
			fill |= fill << 32;
		}

		return fill;
	}

}