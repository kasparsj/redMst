RedMstSessionGUI : RedMstGUI2 {
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
	}
	prDrawInfo { |size|
		// StaticText(win, (size*4)@(size*1.5)).string_("sect:");
		guiSection= StaticText(nil, (size*2)@(size*1.5));
		guiMaxSection= StaticText(nil, (size*3.25)@(size*1.5));
		StaticText(win, (size*4)@(size*1.5)).string_("bpm:");
		guiTempo= StaticText(win, (size*4)@(size*1.5));
		StaticText(win, (size*4)@(size*1.5)).string_("quant:");
		guiQuant= StaticText(win, (size*1.5)@(size*1.5));
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
			var trackW = ((viewWidth - x) / tracks.size).max(50);
			if(tracks.notEmpty, {
				h= size;
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
						Pen.stringAtPoint(seq.beatsAt(section).asString, Point(x, (section+1)*h+5));
						x = x + 20
					};
					// play/stop section
					btn = view.children.select({|c| c.name==("play" ++ section)}).first;
					if (btn.isNil) {
						btn = this.prPlaySectionBtn(view, Rect(x, (section+1)*h, btnW, h*0.9), section)
						.font_(fnt2);
					};
					btn.value_(if (section == RedMst.section or: { section == RedMst.jumpSection } and: { RedMst.isPlaying }, 1, 0));
					x = x + btnW;
					// index
					Pen.fillColor_(colFore2);
					Pen.stringAtPoint(section.asString, Point(x+5, (section+1)*h+5));
					x = x + 20;
					// tracks
					tracks.do{ |trk, i|
						var color = trk.color ?? { Color.hsv(i * 1.0/tracks.size, 1, 1) };
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
							slider = Slider(view, Rect(x + (i*trackW), (numSections+1)*h, (trackW-50).max(50), 150))
							.name_("vol" ++ track.key.asString)
							.value_(track.item.vol)
							.canFocus_(false)
							.action_({|view| track.item.vol = view.value });
						};
						slider.moveTo(x + (i*trackW), (numSections+1)*h);
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
		Pen.color_(color ? colFore);
		Pen.fillRect(Rect(rect.left+margin, rect.top+margin, rect.width-(2*margin),  rect.height-(2*margin)));
		Pen.fillColor_(colFore2);
		Pen.stringAtPoint(name, Point(rect.left+5, rect.top+5));
	}
	prPlaySectionBtn { |view, rect, section|
		^Button(view, rect)
		.name_("play" ++ section)
		.canFocus_(false)
		.states_([["play", colFore, colBack], ["stop", colBack, colFore]])
		.action_{|view| if(view.value==1, { this.prPlay(section) }, { this.prStop })};
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
							((size*2)+25+(lastNumSections*size)+150).min(Window.screenBounds.height-50)
						);
						guiUser.bounds= guiUser.bounds.setExtent(
							win.bounds.width-7,
							(lastNumSections+1)*size+150
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