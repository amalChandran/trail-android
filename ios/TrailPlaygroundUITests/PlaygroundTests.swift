import XCTest

final class PlaygroundTests: XCTestCase {
    @MainActor func testFlightCabAndFerryOnAppleMaps() throws {
        let app=XCUIApplication(); app.launch()
        XCTAssertTrue(app.buttons["openJourneys"].waitForExistence(timeout: 10))
        app.buttons["playPause"].tap(); app.buttons["openJourneys"].tap()
        let play=app.buttons["journeyPlayPause"]
        XCTAssertTrue(play.waitForExistence(timeout: 10))
        func pause() { if play.label=="Pause" { play.tap() } }
        func scroll(_ up: Bool) {
            let start=app.coordinate(withNormalizedOffset: CGVector(dx: 0.98,dy: up ? 0.8 : 0.3))
            start.press(forDuration: 0.05,thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.98,dy: up ? 0.3 : 0.85)))
        }
        func capture(_ name: String) {
            let attachment=XCTAttachment(screenshot: app.screenshot()); attachment.name=name
            attachment.lifetime = .keepAlways; add(attachment)
        }
        func choose(_ title: String,_ count: Int) {
            if !app.buttons["journeyDrawing"].isHittable { scroll(true) }
            app.buttons["journeyDrawing"].tap(); app.buttons[title].tap(); pause()
            XCTAssertTrue(app.staticTexts["journeyGeometry"].label.contains("\(count) points"))
        }
        pause()
        XCTAssertTrue(app.staticTexts["journeyMapStatus"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.staticTexts["journeyMapStatus"].label,"Apple Maps · ready")
        XCTAssertTrue(app.staticTexts["journeyGeometry"].label.contains("129 points"))
        app.sliders["journeyProgress"].adjust(toNormalizedSliderPosition: 0.85)
        capture("Flight — JFK to Heathrow arc on Apple Maps")
        choose("Great circle",129); choose("Two points",2)
        if !app.buttons["journey-cab"].isHittable { scroll(false) }
        app.buttons["journey-cab"].tap(); pause()
        XCTAssertTrue(app.staticTexts["journeyGeometry"].label.contains("119 points"))
        app.sliders["journeyProgress"].adjust(toNormalizedSliderPosition: 0.85)
        capture("Cab — Times Square to Grand Central street route")
        let map=app.maps.firstMatch
        if map.exists { map.pinch(withScale: 1.3,velocity: 1); map.swipeLeft() }
        XCTAssertEqual(app.staticTexts["journeyMapStatus"].label,"Apple Maps · ready")
        XCTAssertEqual(play.label,"Play")
        choose("Arc",129); choose("Full route",119)
        if !app.buttons["journey-ferry"].isHittable { scroll(false) }
        app.buttons["journey-ferry"].tap(); pause()
        XCTAssertTrue(app.staticTexts["journeyGeometry"].label.contains("13 points"))
        app.sliders["journeyProgress"].adjust(toNormalizedSliderPosition: 0.85)
        capture("Ferry — Circular Quay to Manly harbor illustration")
        choose("Two points",2); choose("Arc",129); choose("Full route",13)
        if !app.buttons["journeyStyle"].isHittable { scroll(true) }
        app.buttons["journeyStyle"].tap(); app.buttons["Dashed"].tap()
        app.buttons["journeyMotion"].tap(); app.buttons["Comet"].tap()
        scroll(true)
        let reduced=app.switches["journeyReduced"]
        reduced.coordinate(withNormalizedOffset: CGVector(dx: 0.9,dy: 0.5)).tap()
        XCTAssertEqual(reduced.value as? String,"1")
        app.buttons["journeysDone"].tap()
        XCTAssertTrue(app.buttons["openJourneys"].waitForExistence(timeout: 5))
    }

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
