/*
 * Copyright 2016 Liam Palmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

instance LowerBound[Int16] {
    pub def minValue(): Int16 = Int16.minValue()
}

instance UpperBound[Int16] {
    pub def maxValue(): Int16 = Int16.maxValue()
}

namespace Int16 {

    ///
    /// Returns the number of bits used to represent an `Int16`.
    ///
    pub def size(): Int32 = 16

    ///
    /// Returns the minimum number representable by an `Int16`.
    ///
    pub def minValue(): Int16 = 1i16 <<< (size() - 1)

    ///
    /// Returns the maximum number representable by an `Int16`.
    ///
    pub def maxValue(): Int16 = ~~~minValue()

    ///
    /// Returns the smaller of `x` and `y`.
    ///
    pub def min(x: Int16, y: Int16): Int16 = if (x <= y) x else y

    ///
    /// Returns the larger of `x` and `y`.
    ///
    pub def max(x: Int16, y: Int16): Int16 = if (x >= y) x else y

    ///
    /// Returns the absolute value of `x`.
    /// If the absolute value exceeds maxValue(), -1 is returned.
    ///
    pub def abs(x: Int16): Int16 = {
        if      (x >= 0i16)         x
        else if (x == minValue())   -1i16
        else                        -x
    }

    ///
    /// Returns the distance between `x` and `y`.
    /// If this distance exceeds maxValue(), -1 is returned.
    ///
    pub def dist(x: Int16, y: Int16): Int16 = {
        if      (x >= 0i16 and y >= 0i16)                abs(x - y)
        else if (x < 0i16 and y < 0i16)                  abs(x - y)
        else if (x == minValue() or y == minValue())    -1i16
        else if (minValue() + abs(x) + abs(y) >= 0i16)  -1i16
        else                                            abs(x - y)
    }

    ///
    /// Returns 1 if x > y, -1 if x < y, and 0 if x = y.
    /// The sign of x - y.
    ///
    pub def compare(x: Int16, y: Int16): Int32 = {
        if      (x == y)    0
        else if (x < y)     -1
        else                1
    }

    ///
    /// Returns 1 if x > 0, -1 if x < 0, and 0 if x = 0.
    /// The sign of x.
    ///
    pub def signum(x: Int16): Int32 = compare(x, 0i16)

    ///
    /// Returns the logical right shift of `x` by `distance`.
    /// Only the rightmost 5 bits of `distance` are considered (ie. `distance rem 32`).
    /// A zero is shifted into the leftmost position regardless of sign extension.
    ///
    pub def logicalRightShift(dist: {dist = Int32}, x: Int16): Int16 =
        if (x < 0i16 and dist.dist rem 32 != 0)
            ((x >>> 1) &&& maxValue()) >>> (dist.dist - 1)
        else
            x >>> dist.dist

    ///
    /// Returns the number of one-bits in the two's complement binary
    /// representation of `x`.
    ///
    pub def bitCount(x: Int16): Int32 = {
        if      (x == 0i16)          0
        else if (x rem 2i16 != 0i16) bitCount(logicalRightShift(dist = 1, x)) + 1
        else                         bitCount(logicalRightShift(dist = 1, x))
    }

    ///
    /// Returns the the value obtained by rotating the two's complement
    /// binary representation of `x` right by `distance` bits.
    ///
    pub def rotateRight(dist: {dist = Int32}, x: Int16): Int16 =
        let rem = dist.dist rem size();
        let rot = if (rem >= 0) rem else rem + size();
        (logicalRightShift(dist = rot, x)) ||| (x <<< (size() - rot))

    ///
    /// Returns the the value obtained by rotating the two's complement
    /// binary representation of `x` left by `distance` bits.
    ///
    pub def rotateLeft(dist: {dist = Int32}, x: Int16): Int16 =
        let rem = dist.dist rem size();
        let rot = if (rem >= 0) rem else rem + size();
        (logicalRightShift(dist = size() - rot, x)) ||| (x <<< rot)

    ///
    /// Returns the value obtained by reversing the bits in the
    /// two's complement binary representation of `x`.
    ///
    pub def reverse(x: Int16): Int16 = reverseHelper(x, 0, size()-1)

    ///
    /// Helper function for `reverse`.
    ///
    def reverseHelper(x: Int16, l: Int32, r: Int32): Int16 =
        if (l >= r) x else reverseHelper(swap(x, l, r), l+1, r-1)

    ///
    /// Helper function for `reverse`.
    ///
    def swap(x: Int16, l: Int32, r: Int32): Int16 = match (getBit(pos = l, x), getBit(pos = r, x)) {
        case (1, 0) => clearBit(pos = l, setBit(pos = r, x))
        case (0, 1) => clearBit(pos = r, setBit(pos = l, x))
        case _ => x
    }

    ///
    /// Returns the position of the highest-order/leftmost one-bit in `x`.
    /// Possible return values: 0 (rightmost bit) - 15 (leftmost bit)
    ///                         -1 if x = 0
    ///
    pub def highestOneBitPosition(x: Int16): Int32 =
        // Start at bit 15 and scan right
        oneBitPositionHelper(x, size() - 1, -1)

    ///
    /// Returns the position of the lowest-order/rightmost one-bit in `x`.
    /// Possible return values: 0 (rightmost bit) - 15 (leftmost bit)
    ///                         -1 if x = 0
    ///
    pub def lowestOneBitPosition(x: Int16): Int32 =
        // Start at bit 0 and scan left
        oneBitPositionHelper(x, 0, 1)

    ///
    /// Helper function for highestOneBitPosition and lowestOneBitPosition.
    /// If `x` has a one-bit at `position`, return that position.
    /// Otherwise recursively check the next bit in the same way.
    ///
    def oneBitPositionHelper(x: Int16, position: Int32, delta: Int32): Int32 = {
        if      (position < 0)                   -1
        else if (position > (size() - 1))        -1
        else if (getBit(pos = position, x) == 1) position
        else if (delta == 0)                     -1
        else                                     oneBitPositionHelper(x, position + delta, delta)
    }

    ///
    /// Returns a value with at most a single one-bit, in the position
    /// of the highest-order/leftmost one-bit in `x`.
    /// Returns 0 if x=0.
    ///
    pub def highestOneBit(x: Int16): Int16 =
        bitPositionToInt16(highestOneBitPosition(x))

    ///
    /// Returns a value with at most a single one-bit, in the position
    /// of the highest-order/leftmost one-bit in `x`.
    /// Returns 0 if x=0.
    ///
    pub def lowestOneBit(x: Int16): Int16 =
        bitPositionToInt16(lowestOneBitPosition(x))

    ///
    /// Helper function for highestOneBit and lowestOneBit.
    /// Returns a value with a single one-bit at bit number `position`.
    /// Returns 0 if `position` is outside the range 0-15 inclusive.
    ///
    def bitPositionToInt16(position: Int32): Int16 =
        if (position < 0 or position > size() - 1) 0i16 else 1i16 <<< position

    ///
    /// Returns the number of zero bits preceding the
    /// highest-order/leftmost one-bit in `x`.
    /// Returns 16 if x=0.
    ///
    pub def numberOfLeadingZeros(x: Int16): Int32 =
        if (x == 0i16) size() else size() - 1 - highestOneBitPosition(x)

    ///
    /// Returns the number of zero bits following the
    /// lowest-order/rightmost one-bit in `x`.
    /// Returns 16 if x=0.
    ///
    pub def numberOfTrailingZeros(x: Int16): Int32 =
        if (x == 0i16) size() else lowestOneBitPosition(x)

    ///
    /// Returns the bit of `x` at `pos` (either 0 or 1).
    /// Considers the 5 rightmost bits of `pos` (`pos` mod 32).
    /// The bits of x have positions: 0 (rightmost bit) - 15 (leftmost bit)
    ///
    pub def getBit(pos: {pos = Int32}, x: Int16): Int32 =
        if ((x >>> pos.pos) rem 2i16 == 0i16) 0 else 1

    ///
    /// Returns `x` with the bit at position `pos` set (to 1).
    /// Considers the 5 rightmost bits of `pos` (`pos` mod 32).
    /// The bits of x have positions: 0 (rightmost bit) - 15 (leftmost bit)
    ///
    pub def setBit(pos: {pos = Int32}, x: Int16): Int16 = x ||| (1i16 <<< pos.pos)

    ///
    /// Returns `x` with the bit at position `pos` cleared (to 0).
    /// Considers the 5 rightmost bits of `pos` (`pos` mod 32).
    /// The bits of x have positions: 0 (rightmost bit) - 15 (leftmost bit)
    ///
    pub def clearBit(pos: {pos = Int32}, x: Int16): Int16 = x &&& ~~~(1i16 <<< pos.pos)

    ///
    /// Returns `x` with the bit at position `pos` flipped.
    /// Considers the 5 rightmost bits of `pos` (`pos` mod 32).
    /// The bits of x have positions: 0 (rightmost bit) - 15 (leftmost bit)
    ///
    pub def flipBit(pos: {pos = Int32}, x: Int16): Int16 = x ^^^ (1i16 <<< pos.pos)

    ///
    /// Returns the integer binary logarithm of `x`.
    /// If the given value is 0 or negative, 0 is returned.
    ///
    pub def log2(x: Int16): Int16 =
        if (x <= 0i16) {
            0i16
        } else {
            let position = highestOneBitPosition(x);
            Int32.clampToInt16(min = 0i16, max = Int16.maxValue(), position)
        }

    ///
    /// Returns the factorial of `x`.
    ///
    pub def factorial(x: Int16): Int32 = toInt32(x) |> Int32.factorial

    ///
    /// Return a string representation of `x`.
    ///
    pub def toString(x: Int16): String = ToString.toString(x)

    ///
    /// Parse the string `s` as an Int16, leading or trailing whitespace is trimmed.
    /// A successful parse is wrapped with `Some(x)`, a parse failure is indicated by `None`.
    ///
    pub def fromString(s: String): Option[Int16] = try {
        import java.lang.String.strip(): String \ {};
        import static java.lang.Short.parseShort(String): Int16 \ {};
        Some(s |> strip |> parseShort)
    } catch {
        case _: ##java.lang.NumberFormatException => None
    }

    ///
    /// Convert `x` to an `Option[Int8]`.
    ///
    /// Returns `Some(x as Int8)` if the numeric value of `x` can be represented exactly.
    ///
    /// Returns `None` if the numeric value of `x` is outside the range of Int8
    /// (i.e. -128 to 127).
    ///
    pub def tryToInt8(x: Int16): Option[Int8] =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {};
        import java.lang.Short.byteValue(): Int8 \ {};
        if (x < Int8.toInt16(Int8.minValue()) or x > Int8.toInt16(Int8.maxValue()))
            None
        else
            Some(valueOf(x) |> byteValue)

    ///
    /// Convert `x` to a Int32.
    ///
    /// The numeric value of `x` is preserved exactly.
    ///
    pub def toInt32(x: Int16): Int32 =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {};
        import java.lang.Short.intValue(): Int32 \ {};
        (valueOf(x) |> intValue)

    ///
    /// Convert `x` to a Int64.
    ///
    /// The numeric value of `x` is preserved exactly.
    ///
    pub def toInt64(x: Int16): Int64 =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {};
        import java.lang.Short.longValue(): Int64 \ {};
        (valueOf(x) |> longValue)

    ///
    /// Convert `x` to a BigInt.
    ///
    /// The numeric value of `x` is preserved exactly.
    ///
    pub def toBigInt(x: Int16): BigInt =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {} as i16ValueOf;
        import java.lang.Short.longValue(): Int64 \ {};
        import static java.math.BigInteger.valueOf(Int64): BigInt \ {} as asBigInt;
        (i16ValueOf(x) |> longValue |> asBigInt)

    ///
    /// Convert `x` to a Float32.
    ///
    /// The numeric value of `x` is preserved exactly.
    ///
    pub def toFloat32(x: Int16): Float32 =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {};
        import java.lang.Short.floatValue(): Float32 \ {};
        (valueOf(x) |> floatValue)

    ///
    /// Convert `x` to a Float64.
    ///
    /// The numeric value of `x` is preserved exactly.
    ///
    pub def toFloat64(x: Int16): Float64 =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {};
        import java.lang.Short.doubleValue(): Float64 \ {};
        (valueOf(x) |> doubleValue)

    ///
    /// Convert `x` to a BigDecimal.
    ///
    /// The numeric value of `x` is preserved exactly.
    ///
    pub def toBigDecimal(x: Int16): BigDecimal =
        import new java.math.BigDecimal(Int32): BigDecimal \ {} as fromInt32;
        Int16.toInt32(x) |> fromInt32

    ///
    /// Helper function for the `clamp` conversion function.
    ///
    def clamp(min: {min = Int16}, max: {max = Int16}, x: Int16): Int16 =
        if (x < min.min)
             min.min
        else
            if (x > max.max)
                max.max
            else
                x

    ///
    /// Convert `x` to an `Int8`.
    ///
    /// Returns `x` clamped within the Int8 range `min` to `max`.
    ///
    pub def clampToInt8(min: {min = Int8}, max: {max = Int8}, x: Int16): Int8 =
        import static java.lang.Short.valueOf(Int16): ##java.lang.Short \ {};
        import java.lang.Short.byteValue(): Int8 \ {};
        let mini16 = Int8.toInt16(min.min);
        let maxi16 = Int8.toInt16(max.max);
        (valueOf(clamp(min = mini16, max = maxi16, x)) |> byteValue)

}
