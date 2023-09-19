//redFrik - released under gnu gpl license

RedTrk2 : RedTrk {
	initRedTrk {|argKey, argItem, argSections, argOptions|
		var channels, rate;
		key= argKey;
		argOptions = argOptions ? ();
		if(argItem.isKindOf(Pbind) or: { argItem.isKindOf(Pmono) }, {
			argItem.patternpairs.pairsDo{|key, pat|
				var desc;
				if(key==\instrument, {
					desc= SynthDescLib.global.synthDescs[pat];
					if(desc.notNil, {
						channels= desc.outputs[0].numberOfChannels;//is this correct?
						rate= desc.outputs[0].rate;	//is this correct?
					});
				});
			};
			item= Pbus(argItem, 0, options.fadeTime ? 0.02, channels ? 2, rate ? \audio);
		}, {
			item= argItem;
		});
		sections = argSections.asSequenceableCollection;
		options = argOptions;
		RedMst.add(this);
	}
	fadeTime {
		if (item.respondsTo(\fadeTime)) {
			item.fadeTime;
		};
	}
	fadeTime_ {|time|
		if (item.respondsTo(\fadeTime_)) {
			item.fadeTime_(time);
		};
	}
}
