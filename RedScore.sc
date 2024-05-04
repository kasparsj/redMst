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
		inst.initSections;
		^inst;
	}

	initSections {
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
}
