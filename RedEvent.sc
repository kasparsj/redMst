RedEvent {
	var	<key, <>item, <>sections, <>options;

	*new {|key, item, sections, options|
		var trk= RedMst.at(key);
		if(trk.isNil, {
			^super.new.init(key, item, sections, options);
		}, {
			if(item.isNil, {
				^trk
			}, {
				if (item.kindOf(RedTrk)) {
					("RedMst: replacing track"+key).inform;
					trk.stop;							//stop previous item
				} {
					("RedMst: replacing event"+key).inform;
					trk.stop;							//stop previous item
				};
				trk.item= item;					//swap items
				trk.sections= sections.asSequenceableCollection;
			});
		});
	}
	init {|argKey, argItem, argSections, argOptions|
		key = argKey;
		item = argItem;
		sections = argSections.asSequenceableCollection;
		options = argOptions;
		RedMst.add(this);
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
