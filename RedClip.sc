RedClip {
	var	<key, <>track, <>item, <>sections, <>options;

	*new {|key, track, item, sections, options|
		var clip= RedMst.at(key);
		if(clip.isNil, {
			^super.new.init(key, track, item, sections, options);
		}, {
			if(item.isNil, {
				^clip
			}, {
				if (item.kindOf(RedTrk)) {
					("RedMst: replacing track"++key).inform;
					clip.stop;
				} {
					("RedMst: replacing clip"++key).inform;
					clip.stop;
				};
				clip.item= item; // swap items
				clip.sections= sections.asSequenceableCollection;
			});
		});
	}
	init {|argKey, argTrack, argItem, argSections, argOptions|
		key = argKey;
		track = argTrack;
		item = argItem;
		sections = argSections.asSequenceableCollection;
		options = argOptions;
		RedMst.add(this);
		if (RedMst.at(track).isNil) {
			RedTrk(track);
		};
	}
	addSections { |secs|
		if (secs.isArray.not) {
			secs = [secs];
		};
		secs.do { |sec|
			if (sections.indexOf(sec) == nil) {
				sections = sections.add(sec);
			};
		};
		RedMst.updateMaxSection(sections);
	}
	play {
		if (item.isKindOf(Function)) {
			item.value;
		} {
			item.play;
		};
	}
	stop {
		if (item.respondsTo(\stop)) {
			item.stop;
		}
	}
	printOn {|stream|
		stream<<this.class.name<<$ <<key<<$ <<item.class.name<<$ <<sections
	}
	option { |name|
		^if (options.isKindOf(Dictionary)) {
			options[name];
		} {
			if (options.isArray) {
				options.asDict[name];
			} {
				nil;
			}
		};
	}
	color {
		^this.option(\color);
	}
}
