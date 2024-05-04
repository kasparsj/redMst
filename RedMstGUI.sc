//redFrik - released under gnu gpl license


RedMstGUI {
	var	<win, <>dur= 0.25,
		guiPlay, guiPrev, guiNext, guiSection, guiMaxSection, guiTempo, guiQuant,
		guiMetro, guiUser,
		fnt, colBack, colFore, colBack2, colFore2, task;
	*new {|size= 24, skin|
		^super.new.initRedMstGUI(size, skin);
	}
	initRedMstGUI {|size, skin|
		this.prInitInterface(size, skin);
		this.prInitTask(size);
		this.prInitCleanUp;
	}
	prInitInterface {|size, skin|
		if(skin.isNil, {
			GUI.skins.put(\redMstGUI, (
				background: Color.red(0.8),
				foreground: Color.black,
				fontSpecs: ["Monaco", 9]
			));
			skin= GUI.skins.redMstGUI;
		});
		fnt= Font(skin.fontSpecs[0], size);
		colBack= skin.background;
		colFore= skin.foreground;
		colBack2= colBack.complementary.alpha_(0.3);
		colFore2= colFore.complementary.alpha_(0.7);
		win= Window("RedMst", this.prBounds(size), false).front;
		if(Main.versionAtMost(3, 4) and:{GUI.scheme!=\cocoa}, {
			win.alpha_(skin.unfocus ? 0.9);
		});
		win.view.background_(colBack);
		win.view.decorator= FlowLayout(win.view.bounds);
		win.view.keyDownAction_{|view, char, mod, uni, key|
			case
				{uni==63235 or:{key==124}} {RedMst.next}//right arrow key
				{uni==63234 or:{key==123}} {RedMst.prev}//left arrow key
				{uni==27} {RedMst.next}				//escape key
				{uni==32} {guiPlay.valueAction_(1-guiPlay.value)}//space
			;
		};
		this.prDrawInterface(size);
	}
	prBounds { |size|
		^Rect(300, Window.screenBounds.height-50, size*9.5+20, size*6+25);
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
			.action_{|view| if(view.value==1, {RedMst.play}, {RedMst.stop})};
		guiPrev= Button(win, (size*1.5)@(size*1.5))
			.canFocus_(false)
			.states_([["<", colFore, colBack]])
			.action_{|view| RedMst.prev};
		guiNext= Button(win, (size*1.5)@(size*1.5))
			.canFocus_(false)
			.states_([[">", colFore, colBack]])
			.action_{|view| RedMst.next};
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
						if(RedMst.jumpSection.isNil, {
							guiPrev.states_([["<", colFore, colBack]]);
							guiNext.states_([[">", colFore, colBack]]);
						}, {
							if(RedMst.jumpSection<RedMst.section, {
								guiPrev.states_([["<", colFore2, colBack2]]);
							}, {
								guiNext.states_([[">", colFore2, colBack2]]);
							});
						});
						guiPrev.refresh;
						guiNext.refresh;
					});
				}.defer;
				dur.wait;
			};
		}.play(RedMst.clock);
	}
	prInitCleanUp {
		CmdPeriod.doOnce({
			if(win.isClosed.not, {
				win.close;
				task.stop;
			});
		});
		win.onClose_{task.stop};
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
					Pen.fillColor_(colFore2);
				});
				Pen.addAnnularWedge(
					midPnt,
					0,
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
						if(RedMst.jumpSection.isNil, {
							guiPrev.states_([["<", colFore, colBack]]);
							guiNext.states_([[">", colFore, colBack]]);
						}, {
							if(RedMst.jumpSection<RedMst.section, {
								guiPrev.states_([["<", colFore2, colBack2]]);
							}, {
								guiNext.states_([[">", colFore2, colBack2]]);
							});
						});
						guiPrev.refresh;
						guiNext.refresh;
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
				var w, h, str;
				if(RedMst.items.notEmpty, {
					w= view.bounds.width/(RedMst.maxSection+1);
					h= size;//view.bounds.height/RedMst.items.size;
					Pen.font_(fnt2);
					RedMst.items.do{|trk, y|
						Pen.color_(colFore);
						Pen.strokeRect(Rect(0, y*h, view.bounds.width, h*0.9));
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
						Pen.fillColor_(colFore2);
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
							(size*6+25+(lastNumTracks*size)).min(Window.screenBounds.height-50)
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
							if(RedMst.jumpSection.isNil, {
								guiPrev.states_([["<", colFore, colBack]]);
								guiNext.states_([[">", colFore, colBack]]);
							}, {
								if(RedMst.jumpSection<RedMst.section, {
									guiPrev.states_([["<", colFore2, colBack2]]);
								}, {
									guiNext.states_([[">", colFore2, colBack2]]);
								});
							});
							guiPrev.refresh;
							guiNext.refresh;
						});
					});
				}.defer;
				dur.wait;
			};
		}.play(RedMst.clock);
	}
}

RedMstGUI4 : RedMstGUI2 {
	prInitUser {|size|
		var fnt2= fnt.copy.size_(9);
		guiUser = UserView(win, Rect(0, 0, win.bounds.width-7, 1))
		.drawFunc_{|view|
			var numSections = (RedMst.maxSection+1);
			var currentSection = RedMst.jumpSection ? RedMst.section;
			var w, h, str, trks, btn, trView, volTrk, slider;
			var halfWidth = view.bounds.width/2;
			var btnW = 50;
			if(RedMst.items.notEmpty, {
				h= size;
				Pen.font_(fnt2);
				// sections
				numSections.do{|section|
					trks = RedMst.items.select{ |x|
						x.sections.includes(section);
					};
					str= trks.keys.array.select({|x| x.notNil}).join(", ");

					Pen.color_(colFore);
					Pen.strokeRect(Rect(btnW+5, section*h, halfWidth-btnW-5, h*0.9));
					Pen.fillColor_(colFore2);
					Pen.stringAtPoint(str, Point(btnW+10, section*h));
					Pen.fillColor_(colBack2);
					if (section == RedMst.section) {
						Pen.fillRect(Rect(btnW+5, section*h, halfWidth-btnW-5, h*0.9));
					};
					if(RedMst.isJumping and: { section == RedMst.jumpSection }, {§
						if((RedMst.clock.beats*2).asInteger%2==0, {
							Pen.fillRect(Rect(btnW+5, section*h,  halfWidth-btnW-5, h*0.9));
						});
					});

					btn = view.children.select({|c| c.name==("play" ++ section)}).first;
					if (btn.isNil) {
						btn = Button(view, Rect(0, section*h, btnW, h*0.9))
						.name_("play" ++ section)
						.canFocus_(false)
						.states_([["play", colFore, colBack], ["stop", colBack, colFore]])
						.font_(fnt2)
						.action_{|view| if(view.value==1, {RedMst.play(section)}, {RedMst.stop})};
					};
					btn.value_(if (section == RedMst.section or: { section == RedMst.jumpSection } and: { RedMst.isPlaying }, 1, 0))
				};
				// tracks in current section
				trView = view.children.detect({|c| c.name == "tracks"});
				if (trView.isNil) {
					trView = View(view, Rect(halfWidth, 0, halfWidth, view.bounds.height));
					trView.name = "tracks";
					trks = RedMst.items.select{ |x|
						x.sections.includes(currentSection);
					};
					trks.do {|track, i|
						if (track.item.respondsTo(\vol_)) {
							slider = trView.children.detect({|c| c.name==("vol" ++ track.key.asString)});
							if (slider.isNil) {
								slider = Slider(trView, Rect(btnW+5, i*h, halfWidth-btnW-5, h*0.9))
								.name_("vol" ++ track.key.asString)
								.value_(track.item.vol)
								.canFocus_(false)
								.action_({|view| track.item.vol = view.value });
							};
							slider.moveTo(btnW+5, i*h);
						};
						StaticText(trView, Rect(btnW+5, i*h, halfWidth-btnW-5, h*0.9))
						.font_(fnt2)
						.acceptsMouse_(false)
						.string_(track.key.asString)
						.stringColor_(Color.white)
						.align_(\left);
					};
				};
			});
		};
	}
	prInitTask {|size|
		task= Routine{
			var lastJump, lastSection = RedMst.section, lastNumSections= -1;
			inf.do{
				{
					guiPlay.value_(RedMst.isPlaying.binaryValue);
					guiSection.string_(RedMst.section);
					guiMaxSection.string_("/"++RedMst.maxSection);
					try{
						guiTempo.string_(RedMst.clock.tempo*60);
					} {
						guiTempo.string_("-");
					};
					guiQuant.string_(RedMst.quant);
					if (lastSection != RedMst.section) {
						lastSection = RedMst.section;
						guiUser.children.detect({|c| c.name == "tracks"}).remove;
					};
					if(lastNumSections!=(RedMst.maxSection+1), {
						lastNumSections= (RedMst.maxSection+1);
						win.bounds= win.bounds.setExtent(
							size*25*2+20.min(Window.screenBounds.width - 50),
							(size*6+25+(lastNumSections*size)).min(Window.screenBounds.height-50)
						);
						guiUser.bounds= guiUser.bounds.setExtent(
							win.bounds.width-7,
							lastNumSections*size
						);
					}, {
						guiMetro.refresh;
						guiUser.refresh;
						if(lastJump!=RedMst.jumpSection, {
							lastJump= RedMst.jumpSection;
							if(RedMst.jumpSection.isNil, {
								guiPrev.states_([["<", colFore, colBack]]);
								guiNext.states_([[">", colFore, colBack]]);
							}, {
								if(RedMst.jumpSection<RedMst.section, {
									guiPrev.states_([["<", colFore2, colBack2]]);
								}, {
									guiNext.states_([[">", colFore2, colBack2]]);
								});
							});
							guiPrev.refresh;
							guiNext.refresh;
						});
					});
				}.defer;
				dur.wait;
			};
		}.play(RedMst.clock);
	}
}