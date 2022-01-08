//redFrik - released under gnu gpl license

RedSeq {
	var task;
	*new {|indices, beats, mode|
		^super.new.initRedSeq([indices, beats].flop, mode);
	}
	*newArray {|array, mode|
		^super.new.initRedSeq(array, mode);
	}
	initRedSeq {|array, mode|
		mode = mode ? \beats;
		task= Task({
			array.do{|x|							//x should be [index, beat]
				RedMst.goto(x[0]);
				if (mode == \beats, {
					x[1].wait;
				}, {
					(x[1]*RedMst.clock.tempo).wait;
				});
			};
			RedMst.stop;
			(this.class.name++": sequence finished").postln;
		});
	}
	play {
		task.reset;
		task.play(RedMst.clock);
	}
	stop {
		task.stop;
		RedMst.stop;
	}
	pause {
		task.pause;
	}
	resume {
		task.resume;
	}
}
