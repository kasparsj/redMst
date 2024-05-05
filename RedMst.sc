//redFrik - released under gnu gpl license

//--changes 100704:
//changed from .store and .memStore to .add in helpfiles
//--changes 090904:
//wrote RedSeq
//--changes 090613:
//removed relativeOrigin_(true) in all gui classes
//no longer use the GUI redirect (GUI.button.new became Button) in all gui classes
//--changed 090507:
//added isJumping and jumpSection to RedMst
//various updates to RedMstGUI: resize window, blinking jump section, etc, RedMstGUI2, RedMstGUI3
//--changes 090203:
//added action function to RedMst
//added inf for section index to RedMst
//--changes 081117:
//added class method makeWindow to RedMst
//--changes 081116:
//added RedGrandMst and getState and setState to RedMst
//RedMst no longer stops the clock on .clear - note: this will leave stray clocks running
//--changes 080819:
//added metronome userview
//changed fps to dur for update rate
//--changes 080815:
//added isPlaying flag
//added RedMstGUI class and helpfile
//--changes 080814:
//added skipEmpty flag to jump over empty sections
//--changes 080808:
//took out setters for key in RedTrk
//added the RedTrk2 class - thanks julian for hint about Pbus
//--changes 080807:
//fixed serious bug when 3rd party classes not installed
//fixed duration overlap bug.  added stopAheadTime
//made into a quark
//now works with more items (NodeProxy, BBCut2, RedMOD, RedXM, Function, SynthDef)
//quite a few internal changes - no big changes to the interface
//--071102:
//first release on the mailinglist

//--todo:
//RedSeq.  RedSeqGUI class with esc key for next, section data, playing tracks etc
//test RedTrk and RedTrk2 with Pbindf, Pmono and Pfx
//possible bug with RedTrk2 and addAction.  it now differs from RedTrk
//somehow start quant 0 so one don't have to wait for 1 whole period

//--notes:
//RedXM and RedMod can change RedMst's clock tempo!
//events with long duration does not get cut off when switching section.  use RedTrk2
//don't use BBCut2's CutBuf1 as it will not stop correctly.  replace with CutBuf2 or CutBuf3.

RedMst {
	classvar <items, <>clock, <>quant= 4,
			<section= 0, <maxSection= 0, <jumpSection,
			<>stopAheadTime= 0.05,
			<>skipEmpty= false,
			<>action,
			<isPlaying= false,
			alreadyJumping= false;
	*initClass {
		items= ();
		clock= TempoClock.default;
		CmdPeriod.add({
			alreadyJumping = false;
		});
	}
	*at {|key|
		^items[key];
	}
	*add {|item|
		if (item.isKindOf(RedClip).not) {
			("RedMst.add: invalid item type: " ++ item.class.asString).throw;
		};
		items.put(item.key, item);
		this.updateMaxSection(item.sections);
	}
	*remove {|item|
		item.stop;
		items.put(item.key, nil);
	}
	*clear {
		isPlaying= false;
		items.do{|x| x.clear};
		items= ();
		section= 0;
		maxSection= 0;
		jumpSection= nil;
		action= nil;
		/*if(clock!=TempoClock.default, {
			clock.stop;
			clock.clear;
			clock= TempoClock.default;
		});*/
	}
	*stop {
		isPlaying= false;
		if((clock.notNil && clock.isRunning), {
			clock.schedAbs(clock.nextTimeOnGrid(quant)-stopAheadTime, {
				items.do(_.stop);
				"RedMst: stopped".postln;
				nil;
			});
		}, {
			"RedMst: clock is nil - stopping now".warn;
			items.do(_.stop);
		});
	}
	*play {|startSection= 0|
		this.goto(startSection);
	}
	*goto {|gotoSection|
		isPlaying= true;
		if(clock.isNil, {
			clock= TempoClock.default;
			"RedMst: clock is nil - using TempoClock.default".inform;
		});
		if(alreadyJumping, {
			"RedMst: already jumping somewhere - goto ignored".inform;
		}, {
			jumpSection= gotoSection;
			clock.schedAbs(clock.nextTimeOnGrid(quant)-stopAheadTime, {
				this.prStop(gotoSection);
				nil;
			});
			clock.schedAbs(clock.nextTimeOnGrid(quant), {
				section= gotoSection;
				jumpSection= nil;
				this.prPlay(section);
				alreadyJumping= false;
				action.value;
				nil;
			});
			alreadyJumping= true;
		});
	}
	*tracks {
		^items.select({|x| x.isKindOf(RedTrk) });
	}
	*sectionTracks { |argSection|
		var trackHasClips = ();
		this.sectionClips(argSection).do({|x|
			trackHasClips[x.track] = true;
		});
		argSection = argSection ? section;
		^this.tracks.select{ |x|
			if (x.sections.includes(inf) or: { x.sections.includes(argSection) }) {true} {
				trackHasClips[x.key].notNil;
			}
		};
	}
	*clips {
		^items.select({|x| x.isKindOf(RedTrk).not });
	}
	*sectionClips { |argSection|
		argSection = argSection ? section;
		^items.select{ |x|
			if (x.isKindOf(RedTrk).not) {
				(x.sections.includes(inf) or: { x.sections.includes(argSection) });
			} {false};
		};
	}
	*prStop { |section|
		var clipTracks = this.sectionClips(section).collect({|x| x.track});
		var toStop = items.select{|x|
			(clipTracks.includes(x.key).not and: { x.sections.includes(inf).not and: { x.sections.includes(section).not and:{ x.isPlaying } } });
		};
		toStop.do(_.stop);
	}
	*prPlay { |section|
		var clips = this.sectionClips(section);
		var tracks = this.sectionTracks(section);
		var items = clips ++ tracks;
		if(section>maxSection, {
			("RedMst: section out of range, max: "+maxSection).postln;
		}, {
			("RedMst: play section:"+items.values.collect(_.key).asString+" ("+section+"of"+maxSection+")").postln;
		});
		clips.do(_.play);
		tracks.do {|x|
			if (x.isPlaying.not) {
				x.play;
			};
		};
	}
	*next {
		var jump= section+1;
		if(skipEmpty, {
			while({jump<maxSection and:{items.any{|x| x.sections.includes(jump)}.not}}, {
				jump= jump+1;
			});
		});
		this.goto(jump);
	}
	*prev {
		var jump= section-1;
		if(skipEmpty, {
			while({jump>=0 and:{items.any{|x| x.sections.includes(jump)}.not}}, {
				jump= jump-1;
			});
		});
		this.goto(jump);
	}
	*solo { |trk|
		if (trk.isArray.not, {
			trk = [trk];
		});
		items.do {|t|
			if (trk.includes(t).not) {
				t.mute;
			};
		};
	}
	*mute { |trk|
		if (trk.isNil, {
			trk = items;
		}, {
			if (trk.isArray.not, {
				trk = [trk];
			});
		});
		trk.do { |t|
			if (t.isSymbol) {
				t = this.at(t);
			};
			t.mute;
		}
	}
	*unmute { |trk|
		if (trk.isNil, {
			trk = items;
		}, {
			if (trk.isArray.not, {
				trk = [trk];
			});
		});
		trk.do { |t|
			if (t.isSymbol) {
				t = this.at(t);
			};
			t.unmute;
		}
	}
	*isJumping {
		^jumpSection.notNil;
	}

	//--support for RedMstGUI
	*makeWindow {|size= 24, skin|
		^RedMstGUI(size, skin);
	}

	//--support for RedGrandMst
	*getState {
		^(
			\items: items,
			\clock: clock,
			\quant: quant,
			\section: section,
			\maxSection: maxSection,
			\jumpSection: jumpSection,		//not needed
			\stopAheadTime: stopAheadTime,
			\skipEmpty: skipEmpty,
			\action: action
		)
	}
	*setState {|dict|
		items= dict[\items];
		clock= dict[\clock];
		quant= dict[\quant];
		section= dict[\section];
		maxSection= dict[\maxSection];
		jumpSection= dict[\jumpSection];		//not needed
		stopAheadTime= dict[\stopAheadTime];
		skipEmpty= dict[\skipEmpty];
		action= dict[\action];
	}
	*updateMaxSection { |sections|
		if(sections.size > 0 && sections.includes(inf).not and:{sections.maxItem>maxSection}, {
			maxSection= sections.maxItem;
		});
	}
}
