/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tools.common.flicker.subject.region

import android.tools.common.Timestamp
import android.tools.common.datatypes.Rect
import android.tools.common.datatypes.RectF
import android.tools.common.datatypes.Region
import android.tools.common.flicker.assertions.Fact
import android.tools.common.flicker.subject.FlickerSubject
import android.tools.common.flicker.subject.exceptions.IncorrectRegionException
import android.tools.common.traces.region.RegionEntry

/**
 * Subject for [Region] objects, used to make assertions over behaviors that occur on a rectangle.
 */
class RegionSubject(
    override val parent: FlickerSubject?,
    val regionEntry: RegionEntry,
    override val timestamp: Timestamp
) : FlickerSubject(), IRegionSubject {

    /** Custom constructor for existing android regions */
    constructor(
        region: Region?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(parent, RegionEntry(region ?: Region.EMPTY, timestamp), timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: Array<Rect>,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(Region(rect), parent, timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: Rect?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(Region.from(rect), parent, timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: RectF?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(rect?.toRect(), parent, timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: Array<RectF>,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(mergeRegions(rect.map { Region.from(it.toRect()) }.toTypedArray()), parent, timestamp)

    /** Custom constructor for existing regions */
    constructor(
        regions: Array<Region>,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(mergeRegions(regions), parent, timestamp)

    /** Custom constructor for existing entries */
    constructor(
        regionEntry: RegionEntry?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(regionEntry?.region, parent, timestamp)

    override val selfFacts: List<Fact> = emptyList()

    val region = regionEntry.region

    private val Rect.area
        get() = this.width * this.height

    /** {@inheritDoc} */
    override fun fail(reason: List<Fact>): FlickerSubject {
        val newReason = reason.toMutableList()
        return super.fail(newReason)
    }

    /**
     * Asserts that the current [Region] doesn't contain layers
     *
     * @throws AssertionError
     */
    @Throws(AssertionError::class)
    fun isEmpty(): RegionSubject = apply {check(regionEntry.region.isEmpty) { "Region is empty" } }

    /**
     * Asserts that the current [Region] doesn't contain layers
     *
     * @throws AssertionError
     */
    @Throws(AssertionError::class)
    fun isNotEmpty(): RegionSubject = apply {
        check(regionEntry.region.isNotEmpty) { "Region is not empty" }
    }

    private fun assertLeftRightAndAreaEquals(other: Region) {
        check { MSG_ERROR_LEFT_POSITION }.that(region.bounds.left).isEqual(other.bounds.left)
        check { MSG_ERROR_RIGHT_POSITION }.that(region.bounds.right).isEqual(other.bounds.right)
        check { MSG_ERROR_AREA }.that(region.bounds.area).isEqual(other.bounds.area)
    }

    private fun assertTopBottomAndAreaEquals(other: Region) {
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isEqual(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }.that(region.bounds.bottom).isEqual(other.bounds.bottom)
        check { MSG_ERROR_AREA }.that(region.bounds.area).isEqual(other.bounds.area)
    }

    /** Subtracts [other] from this subject [region] */
    fun minus(other: Region): RegionSubject {
        val remainingRegion = Region.from(this.region)
        remainingRegion.op(other, Region.Op.XOR)
        return RegionSubject(remainingRegion, this, timestamp)
    }

    /** Adds [other] to this subject [region] */
    fun plus(other: Region): RegionSubject {
        val remainingRegion = Region.from(this.region)
        remainingRegion.op(other, Region.Op.UNION)
        return RegionSubject(remainingRegion, this, timestamp)
    }

    /** See [isHigherOrEqual] */
    @Throws(AssertionError::class)
    fun isHigherOrEqual(subject: RegionSubject): RegionSubject = isHigherOrEqual(subject.region)

    /** {@inheritDoc} */
    override fun isHigherOrEqual(other: Rect): RegionSubject = isHigherOrEqual(Region.from(other))

    /** {@inheritDoc} */
    override fun isHigherOrEqual(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isLowerOrEqual(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }
            .that(region.bounds.bottom)
            .isLowerOrEqual(other.bounds.bottom)
    }

    /** See [isLowerOrEqual] */
    @Throws(AssertionError::class)
    fun isLowerOrEqual(subject: RegionSubject): RegionSubject = isLowerOrEqual(subject.region)

    /** {@inheritDoc} */
    override fun isLowerOrEqual(other: Rect): RegionSubject = isLowerOrEqual(Region.from(other))

    /** {@inheritDoc} */
    override fun isLowerOrEqual(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isGreaterOrEqual(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }
            .that(region.bounds.bottom)
            .isGreaterOrEqual(other.bounds.bottom)
    }

    /** {@inheritDoc} */
    override fun isToTheRight(other: Region): RegionSubject = apply {
        assertTopBottomAndAreaEquals(other)
        check { MSG_ERROR_LEFT_POSITION }
            .that(region.bounds.left)
            .isGreaterOrEqual(other.bounds.left)
        check { MSG_ERROR_RIGHT_POSITION }
            .that(region.bounds.right)
            .isGreaterOrEqual(other.bounds.right)
    }

    /** See [isHigher] */
    @Throws(AssertionError::class)
    fun isHigher(subject: RegionSubject): RegionSubject = isHigher(subject.region)

    /** {@inheritDoc} */
    override fun isHigher(other: Rect): RegionSubject = isHigher(Region.from(other))

    /** {@inheritDoc} */
    override fun isHigher(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isLower(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }.that(region.bounds.bottom).isLower(other.bounds.bottom)
    }

    /** See [isLower] */
    @Throws(AssertionError::class)
    fun isLower(subject: RegionSubject): RegionSubject = isLower(subject.region)

    /** {@inheritDoc} */
    override fun isLower(other: Rect): RegionSubject = isLower(Region.from(other))

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     *
     * @throws IncorrectRegionException
     */
    override fun isLower(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isGreater(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }
            .that(region.bounds.bottom)
            .isGreater(other.bounds.bottom)
    }

    /**
     * Asserts that [region] covers at most [testRegion], that is, its area doesn't cover any point
     * outside of [testRegion].
     *
     * @param testRegion Expected covered area
     */
    fun coversAtMost(testRegion: Region): RegionSubject = apply {
        if (!region.coversAtMost(testRegion)) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Out-of-bounds region", region.outOfBoundsRegion(testRegion))
            )
        }
    }

    /** {@inheritDoc} */
    override fun coversAtMost(other: Rect): RegionSubject = coversAtMost(Region.from(other))

    /** {@inheritDoc} */
    override fun notBiggerThan(other: Region): RegionSubject = apply {
        val testArea = other.bounds.area
        val area = region.bounds.area

        if (area > testArea) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Area of test region", testArea),
                Fact("Covered region", region),
                Fact("Area of region", area)
            )
        }
    }

    /** {@inheritDoc} */
    override fun isToTheRightBottom(other: Region, threshold: Int): RegionSubject = apply {
        val horizontallyPositionedToTheRight = other.bounds.left - threshold <= region.bounds.left
        val verticallyPositionedToTheBottom = other.bounds.top - threshold <= region.bounds.top

        if (!horizontallyPositionedToTheRight || !verticallyPositionedToTheBottom) {
            fail(Fact("Region to test", testRegion), Fact("Actual region", region))
        }
    }

    /** {@inheritDoc} */
    override fun coversAtLeast(other: Region): RegionSubject = apply {
        if (!region.coversAtLeast(testRegion)) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Uncovered region", region.uncoveredRegion(testRegion))
            )
        }
    }

    /** {@inheritDoc} */
    override fun coversAtLeast(other: Rect): RegionSubject = coversAtLeast(Region.from(other))

    /** {@inheritDoc} */
    override fun coversExactly(other: Region): RegionSubject = apply {
        val intersection = Region.from(region)
        val isNotEmpty = intersection.op(testRegion, Region.Op.XOR)

        if (isNotEmpty) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Uncovered region", intersection)
            )
        }
    }

    /** {@inheritDoc} */
    override fun coversExactly(other: Rect): RegionSubject = coversExactly(Region.from(other))

    /** {@inheritDoc} */
    override fun overlaps(other: Region): RegionSubject = apply {
        val intersection = Region.from(region)
        val isEmpty = !intersection.op(testRegion, Region.Op.INTERSECT)

        if (isEmpty) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Overlap region", intersection)
            )
        }
    }

    /** {@inheritDoc} */
    override fun overlaps(other: Rect): RegionSubject = overlaps(Region.from(other))

    /** {@inheritDoc} */
    override fun notOverlaps(other: Region): RegionSubject = apply {
        val intersection = Region.from(region)
        val isEmpty = !intersection.op(testRegion, Region.Op.INTERSECT)

        if (!isEmpty) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Overlap region", intersection)
            )
        }
    }

    /** {@inheritDoc} */
    override fun notOverlaps(other: Rect): RegionSubject = apply { notOverlaps(Region.from(other)) }

    /** {@inheritDoc} */
    override fun isSameAspectRatio(other: Region): IRegionSubject {
        val aspectRatio = this.region.width.toFloat() / this.region.height
        val otherAspectRatio = other.region.width.toFloat() / other.region.height
        check { "Aspect Ratio Difference" }
            .that(abs(aspectRatio - otherAspectRatio))
            .isLowerOrEqual(0.1f)
    }

    @Throws(IncorrectRegionException::class)
    fun isSameAspectRatio(other: RegionSubject): IRegionSubject = isSameAspectRatio(other.region)

    companion object {
        const val MSG_ERROR_TOP_POSITION = "Top position"
        const val MSG_ERROR_BOTTOM_POSITION = "Bottom position"
        const val MSG_ERROR_LEFT_POSITION = "Left position"
        const val MSG_ERROR_RIGHT_POSITION = "Right position"
        const val MSG_ERROR_AREA = "Rect area"

        private fun mergeRegions(regions: Array<Region>): Region {
            val result = Region.EMPTY
            regions.forEach { region ->
                region.rects.forEach { rect -> result.op(rect, Region.Op.UNION) }
            }
            return result
        }
    }
}
