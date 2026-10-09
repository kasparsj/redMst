RedMstArrangementGUI : RedMstGUI2 {
	prInitUser {|size|
		var fnt2= fnt.copy.size_(9);
		guiUser= UserView(win, Rect(0, 0, win.bounds.width-7, 1))
			.drawFunc_{|view|
				var w, h, str, color;
				if(RedMst.tracks.notEmpty, {
					w= view.bounds.width/(RedMst.maxSection+1);
					h= size;//view.bounds.height/RedMst.tracks.size;
					Pen.font_(fnt2);
					RedMst.tracks.do{|trk, y|
						color = this.prTrackColor(trk, y, RedMst.tracks.size);
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
						if(lastNumTracks!=RedMst.tracks.size, {
							lastNumTracks= RedMst.tracks.size;
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