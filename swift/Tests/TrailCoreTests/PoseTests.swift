import Testing
import Foundation
import TrailCore
import TrailEffects

@Test func steeringIsContinuousThroughACornerWithoutMovingOffTheRoute() {
    let path=TrailPath([TrailPoint(0,0),TrailPoint(100,0),TrailPoint(100,100)])
    var previous=0.0
    for i in 0...1000 {
        let pose=path.pose(at: Double(i)/1000,headingWindow: 20)!
        #expect(pose.headingRadians >= previous-1e-9)
        #expect(pose.headingRadians-previous < 0.021)
        #expect(pose.point == path.point(at: Double(i)/1000))
        previous=pose.headingRadians
    }
    #expect(abs(previous - .pi/2) < 1e-9)
}

@Test func movingHeadsReportReverseTravelAndStayAtArrivalDuringTrailingEffects() {
    let ping=TrailMotionPreset.pingPong.animation(duration: .seconds(1),repeats: false)
    let forward=ping.sampler.sample(at: TrailTime(progress: 0.25)), reverse=ping.sampler.sample(at: TrailTime(progress: 0.75))
    #expect(forward.head == reverse.head)
    #expect(forward.headDirection == .forward); #expect(reverse.headDirection == .reverse)
    for motion in [TrailMotionPreset.revealThenFlow,.drawAndErase] {
        for p in [0.5,0.75,1.0] { #expect(motion.animation().sampler.sample(at: TrailTime(progress: p)).head == 1) }
    }
}
