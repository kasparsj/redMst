//redFrik - released under gnu gpl license

RedSeq {
	var <currentIndex;
	var <sections;
	var <mode;
	var <scheduler;
	var <waitStart;
	var <waitPause;
	var <>onStop;
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
			(beats*RedMst.clock.tempo);
		}, {
			beats - if (currentIndex > 0, { sections[currentIndex-1][1] }, { 0 });
		});
	}
	play {
		currentIndex = currentIndex ? 0;
		RedMst.goto(this.currentSection);
		waitPause = 0;
		this.prSchedule;
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
	}
	resume {
		this.prSchedule;
	}
	goto { |index|
		currentIndex = index;
		scheduler.clear;
		this.play;
	}
	next {
		if (currentIndex < (sections.size-1), {
			this.goto(currentIndex + 1);
		}, {
			this.stop;
		});
	}
	prev {
		var prev = if (currentIndex > 0, {
			currentIndex - 1;
		}, {
			0;
		});
		this.goto(prev);
	}
	prSchedule {
		waitStart = scheduler.seconds;
		scheduler.sched(this.waitSecs - waitPause, {
			this.next;
		});
	}
}
