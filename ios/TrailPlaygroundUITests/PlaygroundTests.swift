import XCTest

final class PlaygroundTests: XCTestCase {
    @MainActor func testPlaybackAndPluginFlow() throws {
        let app = XCUIApplication(); app.launch()
        let play = app.buttons["playPause"]
        XCTAssertTrue(play.waitForExistence(timeout: 10))
        play.tap(); XCTAssertEqual(play.label, "Play")
        app.sliders["progress"].adjust(toNormalizedSliderPosition: 0.65)
        XCTAssertEqual(play.label, "Play")
        app.buttons["replay"].tap(); XCTAssertEqual(play.label, "Pause")
        play.tap()
        app.buttons["stylePicker"].tap(); app.buttons["Glow"].tap()
        app.buttons["motionPicker"].tap(); app.buttons["Comet"].tap()
        app.swipeUp()
        let plugin = app.switches["customPlugin"]
        XCTAssertTrue(plugin.waitForExistence(timeout: 3)); plugin.tap()
        XCTAssertEqual(plugin.value as? String, "1")
        let reduced = app.switches["reducedMotion"]; reduced.tap()
        XCTAssertEqual(reduced.value as? String, "1")
        let map = app.switches["mapPreview"]; map.tap()
        app.swipeDown()
        XCTAssertTrue(app.buttons["playPause"].exists)
    }
}
