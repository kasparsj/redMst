RedMstSessionGUI : RedMstGUI2 {
	var guiBar;
	// Count from the quantized section start, rather than the GUI's polling time.
	barString {
		var clock = RedMst.clock, start = RedMst.sectionStartBeat;
		var bar, total, index;
		if(RedMst.isPlaying.not or: { start.isNil }, { ^"—" });
		bar = ((clock.beats - start).max(0) / clock.beatsPerBar).floor.asInteger + 1;
		if(seq.notNil and: { seq.mode == \beats }, {
			index = seq.currentIndex;
			// During a queued jump, the sequence already points at its destination.
			if(index.notNil and: { seq.sectionAt(index) == RedMst.section }, {
				total = (seq.beatsAt(index) / clock.beatsPerBar).ceil.asInteger;
			});
		});
		^if(total.notNil, { bar.asString ++ " / " ++ total }, { bar.asString });
	}
	prBounds { |size|
		^Rect(300, Window.screenBounds.height-50, size*30, size*12);
	}
	prDrawInterface { |size|
		this.prDrawInfo(size);
		this.prDrawButtons(size);
		this.prInitMetro(size);
		win.view.decorator.nextLine;

		this.prInitUser(size);

		win.view.children.do{|x|
			if(x.respondsTo(\font), {x.font_(fnt)});
			if(x.respondsTo(\stringColor_), {x.stringColor_(colFore)});
		};
		this.prInitDisplayMove;
	}
	prInitDisplayMove {
		var revision = 0;
		// SC #5166: moving between display scales leaves UserView buffers stale.
		// Wait for movement to settle, then force a real resize of both canvases.
		win.view.onMove_({
			var pending = revision = revision + 1;
			{
				if(win.isClosed.not and: { pending == revision }, {
					[guiUser, guiMetro].do { |view|
						var bounds = view.bounds;
						view.bounds_(bounds.copy.setExtent(bounds.width + 1, bounds.height));
						view.bounds_(bounds);
						view.refresh;
					};
				});
			}.defer(0.2);
		});
	}
	prDrawInfo { |size|
		// StaticText(win, (size*4)@(size*1.5)).string_("sect:");
		guiSection= StaticText(nil, (size*2)@(size*1.5));
		guiMaxSection= StaticText(nil, (size*3.25)@(size*1.5));
		StaticText(win, (size*4)@(size*1.5)).string_("bpm:");
		guiTempo= StaticText(win, (size*4)@(size*1.5));
		StaticText(win, (size*4)@(size*1.5)).string_("quant:");
		guiQuant= StaticText(win, (size*1.5)@(size*1.5));
		win.view.decorator.nextLine;
		StaticText(win, (size*3)@(size*1.5)).string_("bar:");
		guiBar = StaticText(win, (size*8)@(size*1.5)).string_(this.barString);
		win.view.decorator.nextLine;
	}
	prInitUser {|size|
		var fnt2= fnt.copy.size_(9);
		guiUser = UserView(win, Rect(0, 0, win.bounds.width-7, 1))
		.drawFunc_{|view|
			var numSections = (RedMst.maxSection+1);
			var currentSection = RedMst.jumpSection ? RedMst.section;
			var w, h, str, trks, btn, trView, volTrk, slider;
			var viewWidth = view.bounds.width;
			var btnW = 50;
			var tracks = RedMst.tracks.values.sort({|a, b| a.key < b.key});
			var x = btnW + if (seq.notNil, { 40 }, { 20 });
			var trackW = (viewWidth - x).max(0) / tracks.size.max(1);
			if(tracks.notEmpty, {
				h= size.min((view.bounds.height - 150) / (numSections+1)).max(1);
				Pen.font_(fnt2);
				// tracks
				tracks.do {|trk, i|
					this.prDrawTrack(trk.key, Rect(x + (i*trackW), 0, trackW, h*0.9));
				};
				// sections
				numSections.do{|section|
					var clips = RedMst.sectionClips(section);
					x = 0;
					// duration
					if (seq.notNil) {
						Pen.fillColor_(colFore2);
						Pen.stringLeftJustIn(seq.beatsAt(section).asString, Rect(x, (section+1)*h, 20, h*0.9));
						x = x + 20
					};
					// play/stop section
					btn = view.children.select({|c| c.name==("play" ++ section)}).first;
					if (btn.isNil) {
						btn = this.prPlaySectionBtn(view, Rect(x, (section+1)*h, btnW, h*0.9), section)
						.font_(fnt2);
					};
					btn.bounds_(Rect(x, (section+1)*h, btnW, h*0.9));
					btn.value_(if (section == RedMst.section or: { section == RedMst.jumpSection } and: { RedMst.isPlaying }, 1, 0));
					x = x + btnW;
					// index
					Pen.fillColor_(colFore2);
					Pen.stringLeftJustIn(section.asString, Rect(x+5, (section+1)*h, 15, h*0.9));
					x = x + 20;
					// tracks
					tracks.do{ |trk, i|
						var color = this.prTrackColor(trk, i, tracks.size);
						var clips2 = clips.select({|clip| clip.track == trk.key });
						clips2 = if (clips2.size > 0, { clips2 }, { if (trk.sections.includes(section), { [trk] }, []) });
						clips2.do { |clip, j|
							var clipW = trackW / clips2.size;
							this.prDrawTrack(clip.key, Rect(x + (j*clipW), (section+1)*h, clipW, h*0.9), color);
						};
						x = x + trackW;
					};
					// highlight
					Pen.fillColor_(colBack2);
					if (section == RedMst.section) {
						Pen.fillRect(Rect(btnW+20, (section+1)*h, viewWidth-btnW-20, h*0.9));
					};
					if(RedMst.isJumping and: { section == RedMst.jumpSection }, {
						if((RedMst.clock.beats*2).asInteger%2==0, {
							Pen.fillRect(Rect(btnW+20, (section+1)*h,  viewWidth-btnW-20, h*0.9));
						});
					});
				};
				// mixer
				x = btnW + if (seq.notNil, { 40 }, { 20 });
				tracks.do {|track, i|
					if (track.item.respondsTo(\vol_)) {
						slider = view.children.detect({|c| c.name==("vol" ++ track.key.asString)});
						if (slider.isNil) {
							slider = Slider(view, Rect(x + (i*trackW), (numSections+1)*h, (trackW-50).max(1).min(trackW), 150))
							.name_("vol" ++ track.key.asString)
							.value_(track.item.vol)
							.canFocus_(false)
							.action_({|view| track.item.vol = view.value });
							this.prStyleView(slider);
						};
						slider.bounds_(Rect(x + (i*trackW), (numSections+1)*h, (trackW-50).max(1).min(trackW), 150));
					};
					/*StaticText(view, Rect(x + (i*trackW), (numSections+1)*h, trackW, h*0.9))
					.font_(fnt2)
					.acceptsMouse_(false)
					.string_(track.key.asString)
					.stringColor_(Color.white)
					.align_(\left);*/
				};
			});
		};
	}
	prDrawTrack { |name, rect, color, margin=1|
		color = color ? colSurface;
		Pen.color_(color);
		Pen.fillRect(Rect(rect.left+margin, rect.top+margin, rect.width-(2*margin),  rect.height-(2*margin)));
		Pen.fillColor_(this.prTextOn(color));
		Pen.stringLeftJustIn(name, Rect(rect.left+5, rect.top, (rect.width-10).max(0), rect.height));
	}
	prPlaySectionBtn { |view, rect, section|
		var button = Button(view, rect)
		.name_("play" ++ section)
		.canFocus_(false)
		.states_([["play", colFore, colBack], ["stop", colBack, colFore]])
		.action_{|view| if(view.value==1, { this.prPlay(section) }, { this.prStop })};
		this.prStyleView(button);
		^button;
	}
	prInitTask {|size|
		task= Routine{
			var lastJump, lastSection = RedMst.section, lastNumSections= -1;
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
						guiBar.string_(this.barString);
						if (lastSection != RedMst.section) {
							lastSection = RedMst.section;
							guiUser.children.detect({|c| c.name == "tracks"}).remove;
						};
						if(lastNumSections!=(RedMst.maxSection+1), {
							var available = Window.flipY(Window.availableBounds);
							var bounds;
							lastNumSections= (RedMst.maxSection+1);
							bounds= win.bounds.setExtent(
								(size*25*2+20).min(Window.availableBounds.width - 50),
								((size*5)+25+themeHeight+(lastNumSections*size)+150).min(Window.availableBounds.height-50)
							);
							win.bounds= bounds.moveTo(
								bounds.left.clip(available.left+25, available.right-bounds.width-25),
								bounds.top.clip(available.top+25, available.bottom-bounds.height-25)
							);
							guiUser.bounds= guiUser.bounds.setExtent(
								win.bounds.width-7,
								((lastNumSections+1)*size+150).min(win.bounds.height - guiUser.bounds.top - 4)
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