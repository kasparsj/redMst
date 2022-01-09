//redFrik - released under gnu gpl license

RedSeq {
	var task;
	var <currentIndex;
	var <currentSection;
	var <sections;
	*new {|indices, beats, mode|
		^super.new.initRedSeq([indices, beats].flop, mode);
	}
	*newArray {|array, mode|
		^super.new.initRedSeq(array, mode);
	}
	initRedSeq {|array, mode|
		sections = array;
		mode = mode ? \beats;
		task= Task({
			while ({
				currentIndex < (sections.size - 1);
			}, {
				var x = sections[currentIndex];
				currentSection = x[0];
				RedMst.goto(currentSection);
				if (mode == \beats, {
					x[1].wait;
				}, {
					var time = x[1] - if (currentIndex > 0, { sections[currentIndex-1][1] }, { 0 });
					(time*thisThread.clock.tempo).wait;
				});
				currentIndex = currentIndex + 1;
			});
			RedMst.stop;
			(this.class.name++": sequence finished").postln;
		});
	}
	play {
		currentIndex = currentIndex ? 0;
		task.reset;
		task.play(RedMst.clock);
	}
	stop {
		task.stop;
		RedMst.stop;
		currentIndex = nil;
		currentSection = nil;
	}
	pause {
		task.pause;
	}
	resume {
		task.resume;
	}
	goto { |index|
		currentIndex = index;
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
