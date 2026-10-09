//redFrik - released under gnu gpl license


RedMstGUI {
	classvar <theme= \dark, themedWindows;
	var	<win, <>dur= 0.25,
		guiPlay, guiPrev, guiNext, guiSection, guiMaxSection, guiTempo, guiQuant,
		guiMetro, guiUser,
		fnt, colBack, colFore, colBack2, colFore2, task, seq,
		sharedTheme, guiTheme, themeHeight= 0, colSurface, colAccent, viewPalette;
	*initClass { themedWindows = IdentitySet.new }
	*theme_ { |value|
		if([\dark, \light].includes(value).not, {
			"RedMstGUI theme must be \\dark or \\light".throw;
		});
		theme = value;
		{
			themedWindows.copy.do { |gui|
				if(gui.win.isClosed.not, { gui.prApplyTheme });
			};
		}.defer;
	}
	*prThemeSkin {
		^if(theme == \dark, {
			(background: Color(0.12, 0.14, 0.17), foreground: Color(0.88, 0.91, 0.94),
			 surface: Color(0.24, 0.28, 0.33), accent: Color(0.35, 0.65, 0.95),
			 highlight: Color(0.35, 0.65, 0.95, 0.22), fontSpecs: ["Monaco", 9])
		}, {
			(background: Color(0.95, 0.96, 0.97), foreground: Color(0.12, 0.15, 0.19),
			 surface: Color(0.83, 0.86, 0.90), accent: Color(0.16, 0.42, 0.72),
			 highlight: Color(0.16, 0.42, 0.72, 0.18), fontSpecs: ["Monaco", 9])
		});
	}
	*new {|size= 24, skin, seq= nil|
		^super.new.initRedMstGUI(size, skin, seq);
	}
	initRedMstGUI {|size, skin, seq_|
		if (seq_.isKindOf(RedSeq).not) {
			"RedMstGUI seq needs to be of type RedSeq".throw;
		};
		seq = seq_;
		this.prInitInterface(size, skin);
		this.prInitTask(size);
		this.prInitCleanUp;
	}
	prInitInterface {|size, skin|
		sharedTheme = skin.isNil;
		themeHeight = if(sharedTheme, { 28 }, { 0 });
		skin = skin ?? { RedMstGUI.prThemeSkin };
		fnt= Font(skin.fontSpecs[0], size);
		this.prSetColors(skin);
		win= Window("RedMst", this.prBounds(size), false).front;
		if(Main.versionAtMost(3, 4) and:{GUI.scheme!=\cocoa}, {
			win.alpha_(skin.unfocus ? 0.9);
		});
		win.view.background_(colBack);
		win.view.keyDownAction_{|view, char, mod, uni, key|
			case
				{uni==63235 or:{key==124}} {this.prNext}//right arrow key
				{uni==63234 or:{key==123}} {this.prPrev}//left arrow key
				{uni==27} {this.prNext}				//escape key
				{uni==32} {guiPlay.valueAction_(1-guiPlay.value)}//space
			;
		};
		if(sharedTheme, {
			guiTheme = Button(win, Rect(win.view.bounds.width - 68, 4, 64, 22))
				.name_("theme").states_([["Dark"], ["Light"]]).canFocus_(false).resize_(3)
				.action_({ |button| RedMstGUI.theme = [\dark, \light][button.value] });
		});
		win.view.decorator= FlowLayout(win.view.bounds);
		win.view.decorator.shift(0, themeHeight);
		this.prDrawInterface(size);
		if(sharedTheme, {
			themedWindows.add(this);
			guiTheme.font_(Font(skin.fontSpecs[0], 12));
			this.prApplyTheme;
		});
	}
	prSetColors { |skin|
		colBack = skin.background;
		colFore = skin.foreground;
		colBack2 = skin.highlight ?? { colBack.complementary.alpha_(0.3) };
		colFore2 = if(sharedTheme, { colFore }, { colFore.complementary.alpha_(0.7) });
		colSurface = skin.surface ? colFore;
		colAccent = skin.accent ? colFore;
	}
	prApplyTheme {
		var style, skin = RedMstGUI.prThemeSkin;
		if(sharedTheme.not or: { win.isClosed }, { ^this });
		this.prSetColors(skin);
		viewPalette = if(RedMstGUI.theme == \dark, { QPalette.dark }, { QPalette.light });
		viewPalette.window = colBack;
		viewPalette.windowText = colFore;
		viewPalette.button = colSurface;
		viewPalette.buttonText = colFore;
		viewPalette.base = colBack;
		viewPalette.baseText = colFore;
		viewPalette.highlight = colAccent;
		viewPalette.highlightText = this.prTextOn(colAccent);
		win.view.background_(colBack);
		style = { |view|
			this.prStyleView(view);
			view.children.do(style);
		};
		style.value(win.view);
		guiTheme.value_([\dark, \light].indexOf(RedMstGUI.theme));
		this.prUpdateNavigation;
		[guiMetro, guiUser].do { |view| if(view.notNil, { view.refresh }) };
	}
	prStyleView { |view|
		if(sharedTheme.not, { ^this });
		if(viewPalette.notNil, { view.palette_(viewPalette) });
		if(view.isKindOf(StaticText), { view.stringColor_(colFore) });
		if(view.isKindOf(Button), {
			var value = view.value;
			view.states_(if(view === guiTheme, {
				[["Dark", colFore, colSurface], ["Light", colFore, colSurface]]
			}, {
				view.states.collect { |state, i|
					[state[0], if(i == 0, { colFore }, { colBack }),
						if(i == 0, { colBack }, { colFore })]
				}
			}));
			view.value_(value);
		});
		if(view.isKindOf(Slider), {
			view.background_(colSurface).knobColor_(colAccent);
		});
	}
	prUpdateNavigation {
		guiPrev.states_([["<", colFore, colBack]]);
		guiNext.states_([[">", colFore, colBack]]);
		if(RedMst.jumpSection.notNil, {
			if(RedMst.jumpSection < RedMst.section, {
				guiPrev.states_([["<", colFore2, colBack2]]);
			}, {
				guiNext.states_([[">", colFore2, colBack2]]);
			});
		});
	}
	prTrackColor { |track, index, count|
		^track.color ?? {
			Color.hsv(index / count.max(1), 0.45,
				if(sharedTheme and: { RedMstGUI.theme == \light }, { 0.82 }, { 0.55 }))
		};
	}
	prTextOn { |color|
		var channels, luminance;
		if(sharedTheme.not, { ^colFore2 });
		channels = [color.red, color.green, color.blue] * color.alpha
			+ ([colBack.red, colBack.green, colBack.blue] * (1 - color.alpha));
		luminance = (channels.collect { |x|
			if(x <= 0.04045, { x / 12.92 }, { ((x + 0.055) / 1.055).pow(2.4) })
		} * [0.2126, 0.7152, 0.0722]).sum;
		^if(luminance > 0.179, { Color.black }, { Color.white });
	}
	prBounds { |size|
		^Rect(300, Window.screenBounds.height-50, size*9.5+20, size*6+25+themeHeight);
	}
	prDrawInterface { |size|
		this.prDrawButtons(size);
		this.prInitMetro(size);
		win.view.decorator.nextLine;

		this.prDrawInfo(size);
		this.prInitUser(size);

		win.view.children.do{|x|
			if(x.respondsTo(\font), {x.font_(fnt)});
			if(x.respondsTo(\stringColor_), {x.stringColor_(colFore)});
		};
	}
	prDrawButtons { |size|
		guiPlay= Button(win, (size*4)@(size*1.5))
			.canFocus_(false)
			.states_([["play", colFore, colBack], ["stop", colBack, colFore]])
			.action_{|view| if(view.value==1, {this.prPlay}, {this.prStop})};
		guiPrev= Button(win, (size*1.5)@(size*1.5))
			.canFocus_(false)
			.states_([["<", colFore, colBack]])
			.action_{|view| this.prPrev};
		guiNext= Button(win, (size*1.5)@(size*1.5))
			.canFocus_(false)
			.states_([[">", colFore, colBack]])
			.action_{|view| this.prNext};
	}
	prInitMetro {}
	prDrawInfo { |size|
		StaticText(win, (size*4)@(size*1.5)).string_("sect:");
		guiSection= StaticText(win, (size*2)@(size*1.5));
		guiMaxSection= StaticText(win, (size*3.25)@(size*1.5));
		win.view.decorator.nextLine;
		StaticText(win, (size*4)@(size*1.5)).string_("bpm:");
		guiTempo= StaticText(win, (size*4)@(size*1.5));
		win.view.decorator.nextLine;
		StaticText(win, (size*4)@(size*1.5)).string_("quant:");
		guiQuant= StaticText(win, (size*1.5)@(size*1.5));
		win.view.decorator.nextLine;
	}
	prInitUser {}
	prInitTask {|size|
		task= Routine{
			var lastJump;
			inf.do{
				{
					if(win.isClosed.not, {
						guiPlay.value_(RedMst.isPlaying.binaryValue);
						guiSection.string_(RedMst.section);
						guiMaxSection.string_("/"++RedMst.maxSection);
						try{
							guiTempo.string_(RedMst.clock.tempo*60);
						} {
							guiTempo.string_("-");
						};
						guiQuant.string_(RedMst.quant);
						if(lastJump!=RedMst.jumpSection, {
							lastJump= RedMst.jumpSection;
							this.prUpdateNavigation;
						});
					});
				}.defer;
				dur.wait;
			};
		}.play(RedMst.clock);
	}
	prPlay { |section|
		if (seq.notNil, { seq.goto(section) }, { RedMst.play(section) });
	}
	prStop {
		if (seq.notNil, { seq.stop }, { RedMst.stop });
	}
	prNext {
		if (seq.notNil, { seq.next }, { RedMst.next });
	}
	prPrev {
		if (seq.notNil, { seq.prev }, { RedMst.prev });
	}
	prInitCleanUp {
		CmdPeriod.doOnce({
			if(win.isClosed.not, {
				win.close;
				task.stop;
			});
		});
		win.onClose_{task.stop; themedWindows.remove(this)};
	}
}

RedMstGUI2 : RedMstGUI {
	prInitMetro {|size|
		guiMetro= UserView(win, (size*1.5)@(size*1.5))
		.drawFunc_{|view|
			var midPnt= Point(view.bounds.height, view.bounds.width)*0.5;
			var inner= view.bounds.height*0.3;
			var outer= view.bounds.height*0.5;
			var slice= 2pi/RedMst.clock.beatsPerBar;
			Pen.color_(colFore);
			RedMst.clock.beatsPerBar.do{|x|
				Pen.addAnnularWedge(
					midPnt,
					inner,
					outer,
					x*slice+1.5pi,
					slice
				)
			};
			Pen.stroke;
			Pen.addAnnularWedge(
				midPnt,
				inner,
				outer,
				1.5pi,
				RedMst.clock.beatInBar/RedMst.clock.beatsPerBar*2pi
			);
			Pen.fill;
			if(RedMst.isJumping, {
				Pen.fillColor_(if(sharedTheme, { colAccent }, { colFore2 }));
			});
			Pen.addAnnularWedge(
				midPnt,
				0.1,
				inner*0.8,
				1.5pi,
				2pi*(RedMst.quant-(RedMst.clock.nextTimeOnGrid(RedMst.quant)-RedMst.clock.beats))/RedMst.quant;
			);
			Pen.fill;
		};
	}
	prInitTask {|size|
		task= Routine{
			var lastJump;
			inf.do{
				{
					if(win.isClosed.not, {
						guiPlay.value_(RedMst.isPlaying.binaryValue);
						guiSection.string_(RedMst.section);
						guiMaxSection.string_("/"++RedMst.maxSection);
						try{
							guiTempo.string_(RedMst.clock.tempo*60);
						} {
							guiTempo.string_("-");
						};
						guiQuant.string_(RedMst.quant);
						guiMetro.refresh;
						if(lastJump!=RedMst.jumpSection, {
							lastJump= RedMst.jumpSection;
							this.prUpdateNavigation;
						});
					});
				}.defer;
				dur.wait;
			};
		}.play(RedMst.clock);
	}
}

RedMstGUI3 : RedMstGUI2 {
	prInitUser {|size|
		var fnt2= fnt.copy.size_(9);
		guiUser= UserView(win, Rect(0, 0, win.bounds.width-7, 1))
			.drawFunc_{|view|
				var w, h, str, color;
				if(RedMst.items.notEmpty, {
					w= view.bounds.width/(RedMst.maxSection+1);
					h= size;//view.bounds.height/RedMst.items.size;
					Pen.font_(fnt2);
					RedMst.items.do{|trk, y|
						color = this.prTrackColor(trk, y, RedMst.items.size);
						Pen.color_(colSurface);
						Pen.strokeRect(Rect(0, y*h, view.bounds.width, h*0.9));
						Pen.color_(if(sharedTheme, { color }, { colFore }));
						if(trk.sections.includes(inf), {
							(RedMst.maxSection+1).do{|x|
								Pen.fillRect(Rect(x*w, y*h, w, h*0.9));
							};
						}, {
							trk.sections.do{|x|
								Pen.fillRect(Rect(x*w, y*h, w, h*0.9));
							};
						});
						str= trk.key.asString+"("++trk.item.class++")";
						Pen.fillColor_(if(sharedTheme, {
							if(trk.sections.includes(0) or: { trk.sections.includes(inf) }, { this.prTextOn(color) }, { colFore })
						}, { colFore2 }));
						Pen.stringAtPoint(str, Point(0, y*h));
					};
					Pen.fillColor_(colBack2);
					Pen.fillRect(Rect(RedMst.section*w, 0, w, view.bounds.height-(h*0.1)));
					if(RedMst.isJumping, {
						if((RedMst.clock.beats*2).asInteger%2==0, {
							Pen.fillRect(Rect(RedMst.jumpSection*w, 0, w, view.bounds.height-(h*0.1)));
						});
					});
				});
			};
	}
	prInitTask {|size|
		task= Routine{
			var lastJump, lastNumTracks= -1;
			inf.do{
				{
					if(win.isClosed.not, {
						guiPlay.value_(RedMst.isPlaying.binaryValue);
						guiSection.string_(RedMst.section);
						guiMaxSection.string_("/"++RedMst.maxSection);
						try{
							guiTempo.string_(RedMst.clock.tempo*60);
						} {
							guiTempo.string_("-");
						};
						guiQuant.string_(RedMst.quant);
						if(lastNumTracks!=RedMst.items.size, {
							lastNumTracks= RedMst.items.size;
							win.bounds= win.bounds.setExtent(
								win.bounds.width,
								(size*6+25+themeHeight+(lastNumTracks*size)).min(Window.screenBounds.height-50)
							);
							guiUser.bounds= guiUser.bounds.setExtent(
								guiUser.bounds.width,
								lastNumTracks*size
							);
						}, {
							guiMetro.refresh;
							guiUser.refresh;
							if(lastJump!=RedMst.jumpSection, {
								lastJump= RedMst.jumpSection;
								this.prUpdateNavigation;
							});
						});
					});
				}.defer;
				dur.wait;
			};
		}.play(RedMst.clock);
	}
}
