RedScore {
	var <beats;
	var <tracks;
	var <mode;

	*new { |beats_tracks, mode|
		var inst;
		var beats = [];
		var tracks = [];
		beats_tracks.pairsDo { |b, t|
			beats = beats.add(b);
			tracks = tracks.add(t);
		};
		inst = super.newCopyArgs(beats, tracks, mode ? \beats);
		inst.prInit;
		^inst;
	}

	prInit {
		tracks.do { |keys, section|
			if (keys.isArray.not) {
				keys = [keys];
			};
			keys.do { |key|
				if (key != nil) {
					var mst = RedMst.at(key);
					if (mst != nil) {
						mst.addSections(section);
					} {
						"RedScore: section % does not exist".format(key.asString).throw;
					}
				};
			}
		};
	}

	createSeq { |from, to|
		var beats1;
		from = from ? 0;
		to = to ? (this.size - 1);
		if (mode == \beats) {
			beats1 = beats[from..to];
		} {
			beats1 = beats[1..][from..to];
			if (from > 0) {
				beats1 = beats1 - beats[1..][from-1];
			};
		};
		^RedSeq((from..to), beats1, mode);
	}

	size {
		^beats.size;
	}
}
