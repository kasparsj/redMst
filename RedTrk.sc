//redFrik - released under gnu gpl license

RedTrk : RedClip {
	classvar <>playDict, <>stopDict, <>clearDict;
	var	<player, <isPlaying= false, <isMuted = false,
		<>onPlay, <>onStop;

	*initClass {
		playDict= (
			\Pattern: {|item| item.play(RedMst.clock, quant:0)},
			\Stream: {|item| item.reset; item.play(RedMst.clock, quant:0)},
			\BBCut2: {|item| item.play(RedMst.clock)},
			\RedMOD: {|item| item.play(clock:RedMst.clock, quant:0)},
			\RedXM: {|item| item.play(clock:RedMst.clock, quant:0)},
			\RedWindow: {|item| {item.play}.defer; item},
			\Synth: {|item| item.run},
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
	*new {|key, item, sections, options|
		^super.new(key, key, item, sections, options);
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
			if (onPlay.isFunction) {
				onPlay.value;
			};
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
			if (onStop.isFunction) {
				onStop.value;
			};
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
	storeArgs {
		^[key, item, sections]
	}
}
