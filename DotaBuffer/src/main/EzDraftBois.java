package main;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class EzDraftBois {
	private static final Scanner in = new Scanner(System.in);
	private static final Map<Heroes, Map<Heroes, Double>> matchups = new HashMap<>();
	private static final DecimalFormat formatter = new DecimalFormat("##0.00%"), pretty = new DecimalFormat("##0.00");

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Loading...");

		Long start = System.currentTimeMillis();
		int i = 1;

		for (Heroes h : Heroes.values()) {
			Map<Heroes, Double> temp = new HashMap<>();
			matchups.put(h, temp);
			final Elements elems = Jsoup.connect(String.format("http://www.dotabuff.com/heroes/%s/matchups", h.getValue()))
					.data("date", "week")
					.userAgent("Mozilla")
					.timeout(4000)
					.get()
					.body()
					.getElementsByClass("sortable")
					.first()
					.child(1)
					.children()
					.select("[data-link-to]");
			Arrays.stream(Heroes.values())
					.filter(x -> !x.getValue().equals(h.getValue()))
					.forEach(x -> temp.put(x, Double.parseDouble(elems.select("[data-link-to=\"/heroes/" + x.getValue() + "\"]")
							.first()
							.child(2)
							.attr("data-value"))));
			System.out.println(formatter.format(i++ / (double) (Heroes.values().length)) + " complete...");
		}
		System.out.println((System.currentTimeMillis() - start) / 1000D + " seconds elapsed. Done.\nEnter a command:");

		while (loop());
	}

	private static boolean loop() {
		try {
			String str = in.nextLine().trim();
			if (str.equalsIgnoreCase("quit")) {
				System.out.println("Quitting...");
				return false;
			}
			if (str.toLowerCase().startsWith("vs")) {
				str = str.substring(2);
				Heroes a = Heroes.find(str.split(":")[0].trim()).orElse(null), b = Heroes.find(str.split(":")[1].trim()).orElse(null);
				if (null == a || null == b) System.err.println("What fucking hero is that?");
				else System.out.println("\t" + matchups.get(a).get(b));
			} else if (str.toLowerCase().startsWith("team")) {
				str = str.substring(4);
				ArrayList<Heroes> team = Arrays.stream(str.split(","))
						.map(String::trim)
						.map(Heroes::find)
						.map(Optional<Heroes>::get)
						.collect(Collectors.toCollection(ArrayList<Heroes>::new));
				if (team.size() == 1) {
					HashMap<Heroes, Double> temp = new HashMap<>();
					temp.putAll(matchups.get(team.get(0)));
					System.out.println(temp.keySet().stream()
									.sorted((a, b) -> temp.get(b).compareTo(temp.get(a))).limit(5)
									.map(x -> x.getAliases().get(0) + " (" + pretty.format(temp.get(x)) + "%)")
							.collect(Collectors.joining(", ", "Best:\n\t", "\n"))
							+ temp.keySet().stream()
											.sorted((a, b) -> temp.get(a).compareTo(temp.get(b))).limit(5)
											.map(x -> x.getAliases().get(0) + " (" + pretty.format(temp.get(x)) + "%)")
									.collect(Collectors.joining(", ", "Worst:\n\t", "\n"))
							+ "\n");
				} else {
					HashMap<Heroes, Double> total = new HashMap<>();
					total.putAll(matchups.get(team.get(0)));
					for (int i = 1; i < team.size(); i++) {
						Map<Heroes, Double> temp = matchups.get(team.get(i));
						temp.keySet().stream().filter(x -> !team.contains(x)).forEach(x -> total.put(x, total.get(x) + temp.get(x)));
					}

					System.out.println("This team is...\n"
							+ total.keySet().stream().sorted((a, b) -> total.get(b).compareTo(total.get(a))).limit(5)
									.map(x -> x.getAliases().get(0) + " (" + pretty.format(total.get(x)) + "%)")
									.collect(Collectors.joining(", ", "Best against: ", "\n"))
							+ total.keySet().stream().sorted((a, b) -> total.get(a).compareTo(total.get(b))).limit(5)
									.map(x -> x.getAliases().get(0) + " (" + pretty.format(total.get(x)) + "%)")
									.collect(Collectors.joining(", ", "Worst against: ", "\n")));
				}
			} else if (str.toLowerCase().startsWith("get")) {
				Map<Heroes, Double> temp = matchups.get(Heroes.find(str.substring(3).trim()).get());
				System.out.println(temp.keySet().stream().sorted((a, b) -> temp.get(b).compareTo(temp.get(a)))
						.map(y -> String.format("%-25.25s %-10.10s %10.10s", y.getAliases().get(0), "=>", temp.get(y)))
						.collect(Collectors.joining("\n")) + "\n");
			} else if (str.toLowerCase().startsWith("match")) {
				str = str.substring(5);
				ArrayList<Heroes> radiant = Arrays.stream(str.split(":")[0].trim().split(",")).map(String::trim).map(Heroes::find)
						.map(Optional<Heroes>::get).collect(Collectors.toCollection(ArrayList<Heroes>::new)),
						dire = Arrays.stream(str.split(":")[1].trim().split(",")).map(String::trim).map(Heroes::find)
								.map(Optional<Heroes>::get).collect(Collectors.toCollection(ArrayList<Heroes>::new));
				if (radiant.size() == 0 || dire.size() == 0) {
					System.err.println("Usage: match radiant1, radiant2, ... : dire1, dire2, ...");
					return true;
				}
				double radiantTotal = 0, direTotal = 0;
				for (Heroes h : radiant)
					for (Heroes hh : dire)
						radiantTotal += matchups.get(h).get(hh);
				for (Heroes h : dire)
					for (Heroes hh : radiant)
						direTotal += matchups.get(h).get(hh);
				System.out.println(String.format("Radiant (%s%%) : Dire (%s%%) => %s has a %s%% advantage!\n", pretty.format(radiantTotal),
						pretty.format(direTotal), radiantTotal > direTotal ? "Radiant" : "Dire",
						pretty.format((Math.abs(radiantTotal / dire.size()) + Math.abs(direTotal / radiant.size())) / 2)));
			} else if (str.equalsIgnoreCase("help")) {
				String help = "Available commands:\n\n\t" +
						"vs\n\t\t" +
						"Usage: `vs <hero> : <hero>`\n\t\t" +
						"Compares two heroes.\n\n\t" +
						"team\n\t\t" +
						"Usage: `team <hero>[, <hero>, <hero> ...]`\n\t\t" +
						"Shows what the given team is best against and worst against.\n\n\t" +
						"get\n\t\t" +
						"Usage: `get <hero>`\n\t\t" +
						"Shows individual stats for one hero from the past week.\n\n\t" +
						"match\n\t\t" +
						"Usage: `match <hero>[, <hero>, <hero> ...] : <hero>[, <hero>, <hero> ...]`\n\t\t" +
						"Shows the current matchup between two given teams of up to 5 heroes.\n\n\t" +
						"quit\n\t\t" +
						"Quits the application.\n\n\t" +
						"help\n\t\t" +
						"Shows this screen.\n\n";
				System.out.println(help);
			}
		} catch (Exception e) {
			System.err.println("I didn't quite catch that...\nType \"help\" for a list of valid commands.");
		}
		return true;
	}

	private static enum Heroes {
		ABADDON("abaddon", "Abaddon", "Abba", "Abbadon", "Aba"),
		ALCHEMIST("alchemist", "Alchemist", "Al", "Alch", "Alche"),
		ANCIENT_APPARITION("ancient-apparition", "Ancient Apparition", "AA"),
		ANTI_MAGE("anti-mage", "Anti-Mage", "Antimage", "Anti", "AM"),
		ARC_WARDEN("arc-warden", "Arc Warden", "Arc", "AW"),
		AXE("axe", "Axe", "Ax"),
		BANE("bane", "Bane"),
		BATRIDER("batrider", "Batrider", "Bat"),
		BEASTMASTER("beastmaster", "Beastmaster", "Beastie", "Beast", "BM"),
		BLOODSEEKER("bloodseeker", "Bloodseeker", "Blood Cyka", "BloodCyka", "Blood", "Cyka", "BS"),
		BOUNTY_HUNTER("bounty-hunter", "Bounty Hunter", "Bounty", "BH"),
		BREWMASTER("brewmaster", "Brewmaster", "Brew", "Panda"),
		BRISTLEBACK("bristleback", "Bristleback", "Bristle", "BB"),
		BROODMOTHER("broodmother", "Broodmother", "BroodDonger", "Broodmama", "Brood", "Mama"),
		CENTAUR_WARRUNNER("centaur-warrunner", "Centaur Warrunner", "Centaur", "Cent", "CW"),
		CHAOS_KNIGHT("chaos-knight", "Chaos Knight", "Chaos", "CK"),
		CHEN("chen", "Chen"),
		CLINKZ("clinkz", "Clinkz"),
		CLOCKWERK("clockwerk", "Clockwerk", "Cocktwerk", "Clock"),
		CRYSTAL_MAIDEN("crystal-maiden", "Crystal Maiden", "Crystal", "CM"),
		DARK_SEER("dark-seer", "Dark Seer", "Dark", "Seer", "DS"),
		DAZZLE("dazzle", "Dazzle", "Dazz", "Daz"),
		DEATH_PROPHET("death-prophet", "Death Prophet", "Death", "DP"),
		DISRUPTOR("disruptor", "Disruptor", "Disrupt", "Dis"),
		DOOM("doom", "Doom", "Lucifer", "Doom Bringer"),
		DRAGON_KNIGHT("dragon-knight", "Dragon Knight", "Dragon", "DK"),
		DROW_RANGER("drow-ranger", "Drow Ranger", "Drow", "DR"),
		EARTH_SPIRIT("earth-spirit", "Earth Spirit", "Ebola Spirit", "Ebola"),
		EARTHSHAKER("earthshaker", "Earth Shaker", "Earth", "ES"),
		ELDER_TITAN("elder-titan", "Elder Titan", "Elder", "Titan", "ET"),
		EMBER_SPIRIT("ember-spirit", "Ember Spirit", "Ember"),
		ENCHANTRESS("enchantress", "Enchantress", "Enchant", "Ench"),
		ENIGMA("enigma", "Enigma", "Nigma", "Nig"),
		FACELESS_VOID("faceless-void", "Faceless Void", "Faceless", "Void", "FV"),
		GYROCOPTER("gyrocopter", "Gyrocopter", "Gyro", "G"),
		HUSKAR("huskar", "Huskar", "Husk", "H"),
		INVOKER("invoker", "Invoker", "Voker", "Inv"),
		IO("io", "Io"),
		JAKIRO("jakiro", "Jakiro", "Jak"),
		JUGGERNAUT("juggernaut", "Juggernaut", "Jugger", "Jugg", "Jug"),
		KEEPER_OF_THE_LIGHT("keeper-of-the-light", "Keeper of the Light", "Keeper", "KotL"),
		KUNKKA("kunkka", "Kunkka", "Kunks", "Kunk", "Kk"),
		LEGION_COMMANDER("legion-commander", "Legion Commander", "Legion", "LC"),
		LESHRAC("leshrac", "Leshrac", "Lesh"),
		LICH("lich", "Lich"),
		LIFESTEALER("lifestealer", "Lifestealer", "Life", "LS", "Naix"),
		LINA("lina", "Lina"),
		LION("lion", "Lion"),
		LONE_DRUID("lone-druid", "Lone Druid", "Lone", "Druid", "LD"),
		LUNA("luna", "Luna"),
		LYCAN("lycan", "Lycan"),
		MAGNUS("magnus", "Magnus", "Mag"),
		MEDUSA("medusa", "Medusa", "Med", "Dusa"),
		MEEPO("meepo", "Meepo", "Meep"),
		MIRANA("mirana", "Mirana", "PotM"),
		MORPHLING("morphling", "Morphling", "Morph"),
		MONKEY_KING("monkey-king", "Monkey King", "Monkey", "MK"),
		NAGA_SIREN("naga-siren", "Naga Siren", "Naga"),
		NATURES_PROPHET("natures-prophet", "Natures Prophet", "Natures", "Nature's", "Nature", "Prophet", "Proph", "NP"),
		NECROPHOS("necrophos", "Necrophos", "Necrolyte", "Necro"),
		NIGHT_STALKER("night-stalker", "Night Stalker", "Night", "NS", "Batman"),
		NYX_ASSASSIN("nyx-assassin", "Nyx Assassin", "Nyx"),
		OGRE_MAGI("ogre-magi", "Ogre Magi", "Ogre", "Magi"),
		OMNIKNIGHT("omniknight", "Omniknight", "Omni"),
		ORACLE("oracle", "Oracle"),
		OUTWORLD_DEVOURER("outworld-devourer", "Outworld Devourer", "Outhouse Destroyer", "Outsourced Developer", "Outmemed Shitter", "OD"),
		PHANTOM_ASSASSIN("phantom-assassin", "Phantom Assassin", "Mortred", "Morty", "Mort", "PA"),
		PHANTOM_LANCER("phantom-lancer", "Phantom Lancer", "PL"),
		PHOENIX("phoenix", "Phoenix"),
		PUCK("puck", "Puck"),
		PUDGE("pudge", "Pudge"),
		PUGNA("pugna", "Pugna", "Punga"),
		QUEEN_OF_PAIN("queen-of-pain", "Queen of Pain", "Queen", "QoP"),
		RAZOR("razor", "Razor"),
		RIKI("riki", "Riki"),
		RUBICK("rubick", "Rubick", "Rubes", "Rube", "Rub"),
		SAND_KING("sand-king", "Sand King", "Sandy", "Sand", "SK"),
		SHADOW_DEMON("shadow-demon", "Shadow Demon", "Demon", "SD"),
		SHADOW_FIEND("shadow-fiend", "Shadow Fiend", "Shadow Friend", "SF"),
		SHADOW_SHAMAN("shadow-shaman", "Shadow Shaman", "Shaman", "SS"),
		SILENCER("silencer", "Silencer"),
		SKYWRATH_MAGE("skywrath-mage", "Skywrath Mage", "Skywrath", "Chicken", "Sky"),
		SLARDAR("slardar", "Slardar", "Slar"),
		SLARK("slark", "Slark"),
		SNIPER("sniper", "Sniper", "Snipe", "Snip"),
		SPECTRE("spectre", "Spectre", "Spect", "Spec"),
		SPIRIT_BREAKER("spirit-breaker", "Spirit Breaker", "Space Cow", "Cow", "SB"),
		STORM_SPIRIT("storm-spirit", "Storm Spirit", "Storm"),
		SVEN("sven", "Sven"),
		TECHIES("techies", "Techies", "Tech"),
		TEMPLAR_ASSASSIN("templar-assassin", "Templar Assassin", "Templar", "Temp", "Lanaya", "TA"),
		TERRORBLADE("terrorblade", "Terrorblade", "Terror", "TB"),
		TIDEHUNTER("tidehunter", "Tidehunter", "Tide"),
		TIMBERSAW("timbersaw", "Timbersaw", "Timber"),
		TINKER("tinker", "Tinker", "Tink"),
		TINY("tiny", "Tiny", "Tony"),
		TREANT_PROTECTOR("treant-protector", "Treant Protector", "Treant", "TP"),
		TROLL_WARLORD("troll-warlord", "Troll Warlord", "Troll"),
		TUSK("tusk", "Tusk"),
		UNDERLORD("underlord", "Underlord", "Pitlord", "Pit Lord"),
		UNDYING("undying", "Undying", "Undy", "Undies"),
		URSA("ursa", "Ursa"),
		VENGEFUL_SPIRIT("vengeful-spirit", "Vengeful Spirit", "Vengeful", "Venge", "VS"),
		VENOMANCER("venomancer", "Venomancer", "Bananamancer", "Veno"),
		VIPER("viper", "Viper"),
		VISAGE("visage", "Visage", "Vis"),
		WARLOCK("warlock", "Warlock", "Warcock", "War", "Lock"),
		WEAVER("weaver", "Weaver", "Weave"),
		WINDRANGER("windranger", "Windranger", "Windrunner", "Wind", "Lyrelai"),
		WINTER_WYVERN("winter-wyvern", "Winter Wyvern", "Winter", "Wyvern", "WW"),
		WITCH_DOCTOR("witch-doctor", "Witch Doctor", "Witch", "Doc", "WD"),
		WRAITH_KING("wraith-king", "Wraith King", "Wraith", "Skeleton King", "Skele", "WK"),
		ZEUS("zeus", "Zeus", "Zues");

		private final Alias alias;

		private Heroes(String val, String... keys) {
			this.alias = new Alias(val, keys);
		}

		public boolean contains(String str) {
			return alias.matches(str) || alias.getValue().equalsIgnoreCase(str);
		}
		public List<String> getAliases() {
			return alias.getAliases();
		}
		public String getValue() {
			return alias.getValue();
		}

		public static Optional<Heroes> find(String str) {
			for (Heroes h : values())
				if (h.contains(str)) return Optional.of(h);
			return Optional.empty();
		}

		public class Alias {
			private final List<String> keys;
			private final String val;

			public Alias(String val, String... keys) {
				this.val = val;
				this.keys = Arrays.stream(keys).map(String::trim).collect(Collectors.toList());
			}

			public List<String> getAliases() {
				return keys;
			}
			public String getValue() {
				return val;
			}
			public boolean matches(String str) {
				return keys.stream().map(String::toLowerCase).anyMatch(x -> x.equalsIgnoreCase(str));
			}
		}
	}
}
