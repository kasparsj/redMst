//redFrik - released under gnu gpl license

RedSeq {
	var <currentIndex;
	var <sections;
	var <mode;
	var <scheduler;
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
		scheduler.sched(this.waitSecs, {
			this.next;
		});
	}
	stop {
		scheduler.clear;
		RedMst.stop;
		currentIndex = nil;
	}
	pause {
		scheduler.clear;
	}
	resume {
		this.play;
	}
	goto { |index|
		currentIndex = index;
		scheduler.clear;
		this.play;
	}
	next {
		var next = if (currentIndex < (sections.size-1), {
			currentIndex + 1;
		}, {
			0;
		});
		this.goto(next);
	}
	prev {
		var prev = if (currentIndex > 0, {
			currentIndex - 1;
		}, {
			sections.size - 1;
		});
		this.goto(prev);
	}
}
