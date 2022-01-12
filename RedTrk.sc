//redFrik - released under gnu gpl license

RedTrk {
	classvar	<>playDict, <>stopDict, <>clearDict;
	var	<key, <>item, <>sections,
		<player, <isPlaying= false, <isMuted = false;
	*initClass {
		playDict= (
			\Pattern: {|item| item.play(RedMst.clock, quant:0)},
			\Stream: {|item| item.reset; item.play(RedMst.clock, quant:0)},
			\BBCut2: {|item| item.play(RedMst.clock)},
			\RedMOD: {|item| item.play(clock:RedMst.clock, quant:0)},
			\RedXM: {|item| item.play(clock:RedMst.clock, quant:0)},
			\RedWindow: {|item| {item.play}.defer; item}
		);
		stopDict= (
			\Synth: {|player|
				var desc= SynthDescLib.global.synthDescs[player.defName.asSymbol];
				if(desc.notNil and:{desc.hasGate and:{desc.canFreeSynth}}, {
					player.release;
				}, {
					player.free;
				});
			}
		);
		clearDict= (
			\RedWindow: {|player| player.close}
		);
	}
	*new {|key, item, sections|
		var trk= RedMst.at(key);
		if(trk.isNil, {
			^super.new.initRedTrk(key, item, sections);
		}, {
			if(item.isNil, {
				^trk
			}, {
				("RedTrk: replacing track"+key).inform;
				trk.stop;							//stop previous item
				trk.item= item;					//swap items
				trk.sections= sections.asSequenceableCollection;
			});
		});
	}
	initRedTrk {|argKey, argItem, argSections|
		key= argKey;
		item= argItem;
		sections= argSections.asSequenceableCollection;
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
		var func;
		if(isPlaying.not, {
			playDict.keysValuesDo{|key, val|
				if(key.asClass.notNil and:{item.isKindOf(key.asClass)}, {
					func= val;
				});
			};
			if(func.notNil, {
				player= func.value(item);
			}, {
				player= item.play;
			});
			if (isMuted) {
				player.mute;
			};
			isPlaying= true;
		});
	}
	stop {
		var func;
		if(isPlaying, {
			stopDict.keysValuesDo{|key, val|
				if(key.asClass.notNil and:{player.isKindOf(key.asClass)}, {
					func= val;
				});
			};
			if(func.notNil, {
				func.value(player);
			}, {
				player.stop;
			});
			isPlaying= false;
		});
	}
	solo {
		isMuted = false;
		RedMst.solo(this);
	}
	mute {
		isMuted = true;
		if ((isPlaying and: { player.respondsTo(\mute) and: { player.muteCount == 0 }}), {
			player.mute;
		});
	}
	unmute {
		isMuted = false;
		if (isPlaying && player.respondsTo(\unmute)) {
			player.unmute;
		};
	}
	clear {
		var func;
		if(isPlaying, {
			this.stop;
		});
		clearDict.keysValuesDo{|key, val|
			if(key.asClass.notNil and:{player.isKindOf(key.asClass)}, {
				func= val;
			});
		};
		if(func.notNil, {
			func.value(player);
		}, {
			player.free;
		});
	}
	printOn {|stream|
		stream<<this.class.name<<$ <<key<<$ <<item.class.name<<$ <<sections
	}
	storeArgs {
		^[key, item, sections]
	}
}
