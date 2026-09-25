package com.devidea.timeleft.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

// 애니메이션 시간(ms)과 곡선. 같은 카테고리 전환은 같은 값을 쓴다.
//
// - Short: 미세한 상태 변화 (도트 크기, 색 변화)
// - Medium: 일반 전환 (collapse, hover, fade)
// - DetailEntrance: 명시적인 상세 진입의 숫자 카운트, 매초 갱신에는 적용하지 않음
object Motion {
    const val ShortMs = 150
    const val MediumMs = 200
    const val EmphasizedMs = 720
    const val DetailEntranceMs = 1200
    // Large gaps reserve the final 60% for six visible, progressively slower steps.
    const val DetailSettleSteps = 6L
    const val DetailApproachFraction = .4f
    // A clock may need one short borrow before all of its units can count down.
    const val DetailClockBorrowFraction = .15f
    const val StoryChangeMs = 180

    // Used for the approach/borrow phase; the final steps have their own value-based timing.
    val DetailEntranceEasing = CubicBezierEasing(0.2f, 0f, 0.1f, 1f)
}
