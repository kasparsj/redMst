// Isolated Qt theme regression; no audio server or personal startup.
(
var guis, classes, seq, mixer, customGui, customSkin, imageDir, explicitTrackColor;
var check, allViews, selectorFor, sameColor, snapshot, verifySnapshot, capture, themeRegistry;

check = { |condition, message| if(condition.not) { Error(message).throw } };
allViews = { |view|
	var descendants = [view];
	if(view.respondsTo(\children), {
		view.children.do { |child| descendants = descendants ++ allViews.(child) };
	});
	descendants;
};
selectorFor = { |gui|
	allViews.(gui.win.view).detect { |view|
		view.isKindOf(Button) and: { view.name.asString == "theme" }
	};
};
sameColor = { |a, b|
	(a.red - b.red).abs < 0.005
	and: { (a.green - b.green).abs < 0.005 }
	and: { (a.blue - b.blue).abs < 0.005 }
	and: { (a.alpha - b.alpha).abs < 0.005 };
};
snapshot = { |gui|
	var views = allViews.(gui.win.view);
	(views: views, bounds: views.collect(_.bounds), windowBounds: gui.win.bounds);
};
verifySnapshot = { |gui, before, label|
	var after = allViews.(gui.win.view);
	check.(after.size == before[\views].size, label ++ " view count changed");
	after.do { |view, i|
		check.(view === before[\views][i], label ++ " rebuilt view " ++ i);
		check.(view.bounds == before[\bounds][i], label ++ " changed view geometry " ++ i);
	};
	check.(gui.win.bounds == before[\windowBounds], label ++ " changed window geometry");
};
capture = { |gui, className, theme|
	if(imageDir.notNil and: { imageDir.size > 0 }, {
		Image.fromWindow(gui.win).write(imageDir +/+ (className ++ "-" ++ theme ++ ".png"));
	});
};
themeRegistry = {
	var index = RedMstGUI.classVarNames.indexOf(\themedWindows);
	RedMstGUI.classVars[index];
};

Routine {
	try {
		RedMst.clear;
		RedMst.clock = TempoClock.new(8);
		RedMst.quant = 4;
		mixer = RedMstThemeMockPlayer.new;
		explicitTrackColor = Color(0.24, 0.48, 0.66);
		RedTrack(\mixer, mixer, [0, 1], (color: explicitTrackColor));
		RedTrack(\pulse, Tdef(\redMstThemePulse, { inf.wait }), [0, 1]);
		seq = RedSeq([0, 1], [64, 32]);
		classes = [RedMstGUI, RedMstGUI2, RedMstGUI3, RedMstSessionGUI, RedMstArrangementGUI];
		imageDir = "REDMST_THEME_IMAGES".getenv;

		check.(RedMstGUI.theme == \dark, "Theme should default to dark");
		guis = classes.collect { |class| class.new(size: 12, seq: seq) };
		0.8.wait;
		check.(themeRegistry.().size == 5, "Themed window registry did not register all five variants");

		guis.do { |gui, i|
			var selector = selectorFor.(gui);
			check.(selector.notNil, classes[i].name ++ " has no theme selector");
			check.(selector.states.collect(_[0]) == ["Dark", "Light"], classes[i].name ++ " selector states are incorrect");
			check.(selector.value == 0, classes[i].name ++ " did not inherit dark theme");
			check.(sameColor.(gui.win.view.background, Color(0.12, 0.14, 0.17)), classes[i].name ++ " dark background is incorrect");
			check.(sameColor.(gui.prTrackColor(RedMst.at(\mixer), 0, 2), explicitTrackColor), classes[i].name ++ " changed explicit track color");
			check.(selector.bounds.top == 4, classes[i].name ++ " selector is not top-aligned");
			check.(selector.bounds.right == (gui.win.view.bounds.width - 4), classes[i].name ++ " selector is not right-aligned");
			capture.(gui, classes[i].name.asString, "dark");
		};

		// Switching from a window must update every open themed window in place.
		{
			var before = guis.collect(snapshot), darkBackgrounds = guis.collect { |gui| gui.win.view.background };
			selectorFor.(guis[2]).valueAction_(1);
			0.2.wait;
			check.(RedMstGUI.theme == \light, "Selector did not update the shared theme");
			guis.do { |gui, i|
				check.(selectorFor.(gui).value == 1, classes[i].name ++ " selector did not synchronize to light");
				check.(sameColor.(gui.win.view.background, darkBackgrounds[i]).not, classes[i].name ++ " background did not change in light mode");
				check.(sameColor.(gui.win.view.background, Color(0.95, 0.96, 0.97)), classes[i].name ++ " light background is incorrect");
				check.(sameColor.(gui.prTrackColor(RedMst.at(\mixer), 0, 2), explicitTrackColor), classes[i].name ++ " changed explicit track color in light mode");
				verifySnapshot.(gui, before[i], classes[i].name ++ " selector switch");
				capture.(gui, classes[i].name.asString, "light");
			};
		}.value;

		// New windows inherit the active theme, and code switching updates all selectors.
		{
			var inherited = RedMstGUI2(size: 12, seq: seq), inheritedSnapshot;
			0.15.wait;
			check.(selectorFor.(inherited).value == 1, "New window did not inherit light theme");
			inheritedSnapshot = snapshot.(inherited);
			RedMstGUI.theme_(\dark);
			0.2.wait;
			check.(RedMstGUI.theme == \dark, "Code setter did not select dark theme");
			(guis ++ [inherited]).do { |gui|
				check.(selectorFor.(gui).value == 0, "Code setter did not synchronize a selector");
			};
			verifySnapshot.(inherited, inheritedSnapshot, "Inherited window code switch");
			inherited.win.close;
			0.1.wait;
			check.(themeRegistry.().size == 5, "Closed window remained in themed registry");
			RedMstGUI.theme_(\light);
			0.1.wait;
			check.(RedMstGUI.theme == \light, "Theme switch after close failed");
		}.value;

		// Switching during playback must preserve transport, timing, queued jump, bar and mixer state.
		seq.play;
		while { RedMst.sectionStartBeat.isNil } { 0.01.wait };
		0.15.wait;
		{
			var start = RedMst.sectionStartBeat;
			var section = RedMst.section;
			var session = guis[3];
			var bar;
			var slider = allViews.(session.win.view).detect { |view| view.isKindOf(Slider) and: { view.name.asString == "volmixer" } };
			var sliderValue, before;
			check.(RedMst.isPlaying, "Sequence did not start");
			check.(session.barString != "—", "Session bar did not start");
			check.(slider.notNil, "Session mixer slider was not created");
			slider.valueAction_(0.63);
			sliderValue = slider.value;
			before = guis.collect(snapshot);
			RedMst.quant = 16;
			seq.next;
			check.(RedMst.jumpSection.notNil, "Test did not establish a pending jump");
			bar = session.barString;
			{
				var pending = RedMst.jumpSection;
				RedMstGUI.theme_(\dark);
				check.(session.barString == bar, "Theme switch changed the Session bar string");
				0.15.wait;
				check.(RedMst.isPlaying, "Theme switch stopped playback");
				check.(RedMst.section == section, "Theme switch changed current section");
				check.(RedMst.sectionStartBeat == start, "Theme switch changed section start beat");
				check.(RedMst.jumpSection == pending, "Theme switch changed the pending jump");
				check.(slider === allViews.(session.win.view).detect { |view| view.isKindOf(Slider) and: { view.name.asString == "volmixer" } }, "Theme switch rebuilt mixer slider");
				check.((slider.value - sliderValue).abs < 0.0001, "Theme switch changed mixer slider value");
				check.((mixer.vol - sliderValue).abs < 0.0001, "Mixer player lost slider value");
				guis.do { |gui, i| verifySnapshot.(gui, before[i], classes[i].name ++ " playback switch") };
			}.value;
		}.value;
		while { RedMst.jumpSection.notNil } { 0.01.wait };

		// Explicit skins remain private to their window and have no shared selector.
		customSkin = (background: Color(0.71, 0.62, 0.53), foreground: Color(0.08, 0.09, 0.1), fontSpecs: ["Monaco", 9]);
		customGui = RedMstSessionGUI(size: 12, skin: customSkin, seq: seq);
		0.2.wait;
		check.(themeRegistry.().size == 5, "Custom-skinned window entered themed registry");
		check.(selectorFor.(customGui).isNil, "Custom-skinned window should omit theme selector");
		check.(sameColor.(customGui.win.view.background, customSkin.background), "Custom skin background was not applied");
		RedMstGUI.theme_(\light);
		0.15.wait;
		check.(sameColor.(customGui.win.view.background, customSkin.background), "Global switch changed custom skin");

		customGui.win.close;
		guis.do { |gui| gui.win.close };
		0.1.wait;
		check.(themeRegistry.().isEmpty, "Themed registry retained closed windows");
		seq.stop;
		seq.scheduler.stop;
		RedMst.clear;
		RedMst.clock.stop;
		"PASS: shared GUI themes, selectors, inheritance, identity, playback state, mixer, close cleanup, custom skin, and screenshots".postln;
		0.exit;
	} { |error|
		error.reportError;
		1.exit;
	};
}.play(AppClock);
)
