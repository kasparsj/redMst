RedFn {
	var <startFn;
	var <stopFn;

	*new { |startFn, stopFn=nil|
		// todo: validate startFn, stopFn
		^super.newCopyArgs(startFn, stopFn);
	}

	play {
		startFn.value;
	}

	stop {
		if (stopFn.notNil, {
			stopFn.value;
		});
	}
}
