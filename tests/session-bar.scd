// Isolated Qt/clock test; no audio server or personal startup.
(
var gui, seq, check, start, barView, skinGui;
check = { |condition, message| if(condition.not) { Error(message).throw } };
Routine {
	try {
		RedMst.clock = TempoClock.new(8);
		RedMst.quant = 4;
		RedTrack(\bass, Tdef(\bass, { inf.wait }), [0, 1]);
		RedTrack(\melody, Tdef(\melody, { inf.wait }), [0, 1]);
		seq = RedSeq([0, 1], [64, 32]);
		gui = RedMstSessionGUI(seq: seq);
		0.1.wait;
		check.(gui.barString == "—", "Stopped GUI should have no playing bar");
		check.((gui.win.view.background.red - 0.12).abs < 0.005 and: { (gui.win.view.background.blue - 0.17).abs < 0.005 }, "Default background is not neutral");
		seq.play;
		while { RedMst.sectionStartBeat.isNil } { 0.01.wait };
		start = RedMst.sectionStartBeat;
		check.(gui.barString == "1 / 16", "First bar or total is incorrect");
		0.6.wait;
		check.(gui.barString == "2 / 16", "Bar did not advance with the playback clock");
		barView = gui.win.view.children.detect { |view| view.isKindOf(StaticText) and: { view.string == "2 / 16" } };
		check.(barView.notNil, "Visible bar label did not update");
		check.(barView.bounds.bottom < gui.win.view.children.last.bounds.top, "Bar label overlaps the grid");
		seq.next;
		check.(RedMst.sectionStartBeat == start, "Queued jump reset bar before playback");
		while { RedMst.jumpSection.notNil } { 0.01.wait };
		check.(RedMst.sectionStartBeat > start, "Jump did not record a new playback start");
		check.(gui.barString == "1 / 8", "Jump did not reset section bar/total");
		0.3.wait;
		Image.fromWindow(gui.win).write("/tmp/redmst-session-gui.png");
		seq.stop;
		check.(gui.barString == "—", "Stop retained a playing bar");
		gui.win.close;
		0.55.wait;
		seq.goto(1);
		while { RedMst.jumpSection.notNil } { 0.01.wait };
		gui = RedMstSessionGUI(seq: seq);
		check.(gui.barString == "1 / 8", "Opening during playback lost section timing");
		gui.win.close;
		seq.stop;
		0.55.wait;
		skinGui = RedMstSessionGUI(skin: (background: Color.gray(0.7), foreground: Color.black, fontSpecs: ["Monaco", 9]), seq: seq);
		check.((skinGui.win.view.background.red - 0.7).abs < 0.005, "Explicit skin was overridden");
		skinGui.win.close;
		seq.scheduler.stop;
		RedMst.clear;
		check.(RedMst.sectionStartBeat.isNil, "Clear retained stale playback timing");
		RedMst.clock.stop;
		"PASS: session-bar timing, visible updates, jump, stop, reopen, neutral background, and custom skin".postln;
		0.exit;
	} { |error| error.reportError; 1.exit };
}.play(AppClock);
)
