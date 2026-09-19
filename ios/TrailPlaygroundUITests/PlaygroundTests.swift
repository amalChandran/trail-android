import XCTest

final class PlaygroundTests: XCTestCase {
    @MainActor func testCompiledIntegrationExamples() throws {
        let app = XCUIApplication(); app.launch()
        XCTAssertTrue(app.buttons["openExamples"].waitForExistence(timeout: 10))
        app.buttons["playPause"].tap()
        app.buttons["openExamples"].tap()
        XCTAssertTrue(app.buttons["examplePicker"].waitForExistence(timeout: 5))
        func choose(_ title: String) {
            app.buttons["examplePicker"].tap()
            app.buttons[title].tap()
            XCTAssertTrue(app.staticTexts["exampleCode"].exists)
            XCTAssertFalse(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "Example source missing")).firstMatch.exists)
        }
        choose("Runtime color")
        app.buttons["changeBrandColor"].tap()
        let exampleScreenshot = XCTAttachment(screenshot: app.screenshot())
        exampleScreenshot.name = "Runtime color and compiled source"
        exampleScreenshot.lifetime = .keepAlways
        add(exampleScreenshot)
        choose("Custom plugin")
        app.buttons["changeBrandColor"].tap()
        choose("Playback controls")
        app.sliders["exampleProgress"].adjust(toNormalizedSliderPosition: 0.4)
        XCTAssertEqual(app.buttons["examplePlayPause"].label, "Play")
        app.buttons["exampleReplay"].tap()
        app.sliders["exampleProgress"].adjust(toNormalizedSliderPosition: 0.7)
        XCTAssertEqual(app.buttons["examplePlayPause"].label, "Play")
        for title in ["Sequence", "Layers", "MapKit", "Existing SwiftUI Map", "Existing MKMapView"] { choose(title) }
        app.buttons["examplesDone"].tap()
        XCTAssertTrue(app.buttons["playPause"].waitForExistence(timeout: 5))
    }

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
