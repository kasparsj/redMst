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
			var events = RedMst.events.values.sort({|a, b| a.key < b.key});
			var trackW = ((viewWidth - btnW - 20) / (tracks.size + 1)).max(50);
			if(tracks.notEmpty, {
				h= size;
				Pen.font_(fnt2);
				// tracks
				tracks.do {|x, i|
					this.prDrawTrack(x.key, Rect(btnW+20 + (i*trackW), 0, trackW, h*0.9));
				};
				// events track
				this.prDrawTrack("Events", Rect(btnW+20 + (tracks.size*trackW), 0, trackW, h*0.9));
				// sections
				numSections.do{|section|
					var evts, evW;
					// index
					Pen.fillColor_(colFore2);
					Pen.stringAtPoint(section.asString, Point(0, (section+1)*h+5));
					// tracks
					tracks.do{ |x, i|
						if (x.sections.includes(section)) {
							this.prDrawTrack(x.key, Rect(btnW+20 + (i*trackW), (section+1)*h, trackW, h*0.9));
						};
					};
					// events
					evts = events.select({|x|
						(x.sections.includes(section));
					});
					evW = trackW / evts.size;
					evts.do{ |x, i|
						this.prDrawTrack(x.key, Rect(btnW+20 + (tracks.size*trackW) + (i*evW), (section+1)*h, evW, h*0.9));
					};
					Pen.fillColor_(colBack2);
					if (section == RedMst.section) {
						Pen.fillRect(Rect(btnW+20, (section+1)*h, viewWidth-btnW-20, h*0.9));
					};
					if(RedMst.isJumping and: { section == RedMst.jumpSection }, {
						if((RedMst.clock.beats*2).asInteger%2==0, {
							Pen.fillRect(Rect(btnW+20, (section+1)*h,  viewWidth-btnW-20, h*0.9));
						});
					});

					btn = view.children.select({|c| c.name==("play" ++ section)}).first;
					if (btn.isNil) {
						btn = this.prPlaySectionBtn(view, Rect(15, (section+1)*h, btnW, h*0.9), section)
						.font_(fnt2);
					};
					btn.value_(if (section == RedMst.section or: { section == RedMst.jumpSection } and: { RedMst.isPlaying }, 1, 0))
				};
				// tracks in current section
				// trView = view.children.detect({|c| c.name == "tracks"});
				// if (trView.isNil) {
				// 	trView = View(view, Rect(halfWidth, 0, halfWidth, view.bounds.height));
				// 	trView.name = "tracks";
				// 	trks = RedMst.tracks.select{ |x|
				// 		x.sections.includes(currentSection);
				// 	};
				// 	trks.do {|track, i|
				// 		if (track.item.respondsTo(\vol_)) {
				// 			slider = trView.children.detect({|c| c.name==("vol" ++ track.key.asString)});
				// 			if (slider.isNil) {
				// 				slider = Slider(trView, Rect(btnW+5, i*h, halfWidth-btnW-5, h*0.9))
				// 				.name_("vol" ++ track.key.asString)
				// 				.value_(track.item.vol)
				// 				.canFocus_(false)
				// 				.action_({|view| track.item.vol = view.value });
				// 			};
				// 			slider.moveTo(btnW+5, i*h);
				// 		};
				// 		StaticText(trView, Rect(btnW+5, i*h, halfWidth-btnW-5, h*0.9))
				// 		.font_(fnt2)
				// 		.acceptsMouse_(false)
				// 		.string_(track.key.asString)
				// 		.stringColor_(Color.white)
				// 		.align_(\left);
				// 	};
				// };
			});
		};
	}
	prDrawTrack { |name, rect|
		Pen.color_(colFore);
		Pen.strokeRect(rect);
		Pen.fillColor_(colFore2);
		Pen.stringAtPoint(name, Point(rect.left+5, rect.top));
	}
	prPlaySectionBtn { |view, rect, section|
		^Button(view, rect)
		.name_("play" ++ section)
		.canFocus_(false)
		.states_([["play", colFore, colBack], ["stop", colBack, colFore]])
		.action_{|view| if(view.value==1, {RedMst.play(section)}, {RedMst.stop})};
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