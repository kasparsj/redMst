//redFrik - released under gnu gpl license

RedSeq {
	var <currentIndex;
	var <sections;
	var <mode;
	var <scheduler;
	var <waitStart;
	var <waitPause;
	var <>onStop;
	var <>onLoop;
	var <>onGoto;
	var <>onPause;
	var <>onResume;
	*new {|indices, beats, mode|
		^super.new.initRedSeq([indices, beats].flop, mode);
	}
	*newArray {|array, mode|
		^super.new.initRedSeq(array, mode);
	}
	initRedSeq {|array, m|
		sections = array;
		mode = m ? \beats;
		scheduler = TempoClock.new;
	}
	currentSection {
		^sections[currentIndex][0];
	}
	waitSecs {
		var beats = sections[currentIndex][1];
		^if (mode == \beats, {
			(beats * (1.0 / RedMst.clock.tempo));
		}, {
			beats - if (currentIndex > 0, { sections[currentIndex-1][1] }, { 0 });
		});
	}
	play {
		currentIndex = currentIndex ? 0;
		RedMst.goto(this.currentSection);
		waitPause = 0;
		this.resume;
	}
	stop {
		scheduler.clear;
		RedMst.stop;
		currentIndex = nil;
		if (onStop != nil) {
			onStop.value;
		};
	}
	pause {
		waitPause = waitPause + (scheduler.seconds - waitStart);
		scheduler.clear;
		if (onPause != nil) {
			onPause.value;
		};
	}
	resume {
		this.prSchedule;
		if (onResume != nil) {
			onResume.value;
		};
	}
	goto { |index, fromSched|
		currentIndex = index;
		scheduler.clear;
		this.play;
		if (onGoto != nil) {
			onGoto.value(fromSched);
		};
	}
	next { |fromSched|
		if (currentIndex.notNil) {
			if (currentIndex < (sections.size-1), {
				this.goto(currentIndex + 1, fromSched);
			}, {
				this.stop;
				if (onLoop != nil) {
					onLoop.value;
				}
			});
		}
	}
	prev { |fromSched|
		var prev = if (currentIndex > 0, {
			currentIndex - 1;
		}, {
			0;
		});
		this.goto(prev, fromSched);
	}
	prSchedule {
		waitStart = scheduler.seconds;
		scheduler.sched(this.waitSecs - waitPause, {
			this.next(true);
		});
	}
}
