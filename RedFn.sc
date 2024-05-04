RedFn {
	var <startFn;
	var <stopFn;

	*new { |startFn, stopFn|
		// todo: validate startFn, stopFn
		^super.newCopyArgs(startFn, stopFn);
	}

	play {
		startFn.value;
	}

	stop {
		stopFn.value;
	}
}
