// Isolated GUI regression: no audio server or personal startup scripts.
(
var gui, seq, canvases, originalBounds, counts, check;
check = { |condition, message| if(condition.not) { Error(message).throw } };
Routine {
	try {
		RedTrack(\test, Tdef(\test, { inf.wait }), [0]);
		seq = RedSeq([0], [4]);
		gui = RedMstSessionGUI(seq: seq);
		0.5.wait;
		canvases = gui.win.view.children.select { |view| view.isKindOf(UserView) };
		check.(canvases.size == 2, "Expected grid and metronome canvases");
		originalBounds = canvases.collect { |view| view.bounds };
		counts = Array.fill(2, 0);
		canvases.do { |view, i| view.onResize_({ counts[i] = counts[i] + 1 }) };
		// A burst of move notifications should rebuild buffers only once.
		3.do { gui.win.view.moveEvent; 0.04.wait };
		0.4.wait;
		check.(counts.every { |count| count == 2 }, "Move burst was not debounced");
		check.(canvases.collect(_.bounds) == originalBounds, "Canvas geometry drifted");
		check.(seq.currentIndex.isNil and: { RedMst.isPlaying.not }, "Movement changed playback");
		// A later move must still rebuild both buffers.
		gui.win.view.moveEvent;
		0.4.wait;
		check.(counts.every { |count| count == 4 }, "Later move did not resize both canvases");
		check.(canvases.collect(_.bounds) == originalBounds, "Repeated move changed geometry");
		// Closing with a pending callback must not touch destroyed views.
		gui.win.view.moveEvent;
		gui.win.close;
		0.4.wait;
		check.(counts.every { |count| count == 4 }, "Closed canvases were resized");
		seq.scheduler.stop;
		"PASS: display-move debounce, both canvas resizes, geometry, playback, and close safety".postln;
		0.exit;
	} { |error|
		error.reportError;
		1.exit;
	};
}.play(AppClock);
)
