RedVol {
	var <item;
	var <volFn;
	var <vol;

	*new { |item, volFn, vol|
		// todo: validate item, volFn
		var inst = super.newCopyArgs(item, volFn);
		inst.vol = vol ? 1;
		^inst;
	}

	play {
		item.play;
	}

	stop {
		item.stop;
	}

	vol_ { |val|
		vol = val;
		volFn.value(vol);
	}
}