import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.Random;
import javax.swing.JApplet;

public class M extends JApplet implements Runnable, MouseListener, KeyListener {
	MouseEvent click = null;
	boolean key[] = new boolean[65535];
	BufferStrategy strategy;
	Random r = new Random();

	static String n(int p) {
		return new String(new char[] {
			(char) ('A' + (p + 5) % 7),
			new char[] {'u','e','y','o','i'}[(p + 2) % 5],
			(char) ('k' + (p / 3) % 4),
			new char[] {'u','e','i','o','a'}[(p / 2 + 1) % 5],
			(char) ('p' + (p / 2) % 9)
		}) + new String[] { " I", " II", " III", " IV", " V", " VI" }[(p / 4 + 3) % 6];
	}

	@Override
	public void init() {
		setIgnoreRepaint(true);
		Canvas canvas = new Canvas();
		add(canvas);
		canvas.setBounds(0, 0, 712, 600);
		canvas.createBufferStrategy(2);
		strategy = canvas.getBufferStrategy();
		canvas.addMouseListener(this);
		canvas.addKeyListener(this);
		new Thread(this).start();
	}

	String[] a_names = {
		"Explore",
		"build Outpost",
		"Colonise",
		"Tax",
		"improve Defences",
		"trAde",
		"Raid",
		"Invade",
		"build Warship",
		"develop advanced fuels",
		"develop terraforming",
		"develop cloaking device",
		"develop advanced economics",
		"develop advanced weapons",
		"develop long-range scanners",
		"build transcendence device"
		//"get max income",
		//"smartinvade",
		//"upgrade planets if needed"
	};

	int[] ai = {
		15, // transcend

		10, // terraform
		12, // adv econ
		13, // adv weap
		9,  // adv fuels
		11, // cloak
		14, // scanners

		24, // defences
		0,  // explore
		17, // smart invade
		2,  // colonise
		8,  // warship
		1,  // outpost

		16, // income
		-1  // guard
	};

	int[] a_shortcuts = {
		KeyEvent.VK_E,
		KeyEvent.VK_O,
		KeyEvent.VK_C,
		KeyEvent.VK_T,
		KeyEvent.VK_D,
		KeyEvent.VK_A,
		KeyEvent.VK_R,
		KeyEvent.VK_I,
		KeyEvent.VK_W
	};

	boolean doAction(int a) {
		if (allowed(a) && e_money[selE] + value(a) >= 0) {
			e_msg_fromto[selE][selE] = "";
			e_money[selE] += value(a);
			goback: while(true) { switch (a) {
				// Skip turn
				case -1: break;
				// Explore
				case 0:
					p_explored[selP][selE] = true;
					e_msg_fromto[selE][selE] = "You explore " + n(selP) +
							" (" + s_names[p_special[selP]] + ", " + e_names[p_owner[selP]] + ")";
					break;
				// Outpost
				case 1:
					p_owner[selP] = selE;
					p_out[selP] = true;
					p_defence[selP] = 1;
					break;
				// Colony
				case 2:
					p_owner[selP] = selE;
					p_out[selP] = false;
					if (p_special[selP] == 5) {
						// Warships
						e_ships[selE]++;
					}
					if (p_special[selP] == 6) {
						// Defensible
						p_defence[selP] += 2;
					}
					if (p_special[selP] == 4) {
						String what = " nothing of interest";
						int option = r.nextInt(6);
						opts: for (int o = option; o < option + 6; o++) { switch (o % 6) {
							case 0:
							// Advanced Fuels
							if (e_range[selE] == 5) {
								e_range[selE] = 10;
								what = " advanced fuel technology";
								break opts;
							}
							case 1:
							// Terraforming
							if (!e_terraform[selE]) {
								e_terraform[selE] = true;
								what = " terraforming technology";
								break opts;
							}
							case 2:
							// Cloak
							if (!e_cloak[selE]) {
								e_cloak[selE] = true;
								what = " cloaking technology";
								break opts;
							}
							case 3:
							// Economics
							if (e_econBonus[selE] == 0) {
								e_econBonus[selE] = 1;
								what = " advanced economics textbooks";
								break opts;
							}
							case 4:
							// Weapons
							if (e_gunBonus[selE] == 0) {
								e_gunBonus[selE] = 1;
								what = " advanced weapons technology";
								break opts;
							}
							// Scanners
							case 5:
							if (!e_scanner[selE]) {
								e_scanner[selE] = true;
								for (int p = 0; p < 24; p++) { p_explored[p][selE] = true; }
								what = " detailed planetary charts";
								break opts;
							}
						}}
						e_msg_fromto[selE][selE] = "You discover" + what + " on " + n(selP);
					}
					break;
				// Raid
				case 6:
					e_msg_fromto[selE][p_owner[selP]] = e_names[selE] + " raids " + n(selP) +
							" for $" + p_money[selP];
				// Tax, Raid
				case 3:
					p_money[selP] = 0; break;
				// Improve Defences
				default: case 4: p_defence[selP] += 4; break;
				// Trade
				case 5:
					e_money[p_owner[selP]] += p_money[selP];
					e_msg_fromto[selE][p_owner[selP]] = e_names[selE] + " trades with you for $" +
							p_money[selP];
					p_money[selP] = 0;
					break;
				// Invade
				case 17: case 7:
					int victim = p_owner[selP];
					e_msg_fromto[selE][victim] = n(selP) + " is invaded by " + e_names[selE]
							+ ", taking $" + p_money[selP];
					int def = p_defence[selP] * 3 / 4;
					if (e_f_pos[victim] == selP) {
						def += e_ships[victim];
						e_ships[victim] = e_ships[victim] / 2;
						p_owner[selP] = selE;
						// Find a haven for them.
						for (int p = 0; p < 24; p++) {
							if (p_owner[p] == victim) {
								e_f_pos[victim] = p;
								e_msg_fromto[selE][victim] += ". Your ships fled to " + n(p);
								break;
							}
						}
					}
					p_owner[selP] = selE;
					p_defence[selP] = p_defence[selP] * 3 / 4;
					int shipsLost = def - e_ships[selE] / 3;
					e_msg_fromto[selE][selE] = "You invade " + n(selP) + ", taking " +
						p_money[selP] + "$";
					e_money[selE] += p_money[selP];
					p_money[selP] = 0;
					if (shipsLost > 0) {
						if (shipsLost > e_ships[selE] / 2) { shipsLost = e_ships[selE] / 2; }
						e_ships[selE] -= shipsLost;
						e_msg_fromto[selE][selE] += " and losing " + shipsLost + " warships";
					}
					e_f_pos[selE] = selP;
					break;
				// Build Warship
				case 8: e_ships[selE]++; break;
				// Advanced Fuels
				case 9: e_range[selE] = 10; break;
				// Terraforming
				case 10: e_terraform[selE] = true; break;
				// Cloak
				case 11: e_cloak[selE] = true; break;
				// Economics
				case 12: e_econBonus[selE]++; break;
				// Weapons
				case 13: e_gunBonus[selE]++; break;
				// Scanners
				case 14:
					e_scanner[selE] = true;
					for (int p = 0; p < 24; p++) { p_explored[p][selE] = true; }
					break;
				// Transcend
				case 15: e_transcend[selE] = true; break;
				// Max money
				case 16:
					if (p_owner[selP] == selE) {
						a = 3; // tax
					} else {
						a = e_cloak[selE] ? 6 : 5; // Raid if possible, otherwise trade
					}
					continue goback; // GOTO! WHEEE!
			} break; } // This essentially implements goto. Yes, cry. Cry now.
			// HAVE THEY WON OR LOST?
			for (int e = 1; e < 5; e++) {
				e_lost[e] = true;
				for (int p = 0; p < 24; p++) {
					if (p_owner[p] == e) { e_lost[e] = false; }
				}
				if (!e_lost[e]) {
					e_won[e] = true;
					for (int p = 0; p < 24; p++) {
						if (p_owner[p] != e && p_owner[p] != 0) { e_won[e] = false; }
					}
					if (e_transcend[e]) {
						e_won[e] = true;
					}
				}
			}

			if (!e_won[selE]) {
				// END TURN
				for (int p = 0; p < 24; p++) {
					if (p_owner[p] != 0 && !p_out[p]) {
						int money = 2;
						switch (p_special[p]) {
							// Rich
							case 2: money = 3; break;
							// Poor
							case 3: money = 1;
						}
						p_money[p] += (money + e_econBonus[p_owner[p]]);
					}
				}
				e_p_sel[selE] = selP;
				selE++;
				if (selE == 5) { selE = 1; }
				selP = e_p_sel[selE];
				// Clear outbox
				for (int i = 0; i < 5; i++) {
					e_msg_fromto[selE][i] = selE == i ? e_msg_fromto[selE][i] : "";
				}
				antechamber = needAntechamber();
				// Stipend!
				//if (!e_human[selE]) { e_money[selE] += 50; }
			}
			return true;
		}
		return false;
	}

	boolean allowed(int a) {
		switch (a) {
			// Skip
			case -1: return true;
			// Explore
			case 0: return inRange() && !p_explored[selP][selE];
			// Outpost
			case 1: return inRange() && p_explored[selP][selE] && p_owner[selP] == 0;
			// Colony
			case 2:
				return 
					inRange() &&
					p_explored[selP][selE] &&
					(p_owner[selP] == 0 || (p_owner[selP] == selE && p_out[selP])) &&
					(p_special[selP] != 0 || e_terraform[selE]);
			// Tax, Improve Defences
			case 3: case 4: return p_owner[selP] == selE;
			// Raid
			case 6: if (!e_cloak[selE] || e_ships[selE] == 0) { return false; } // NO BREAK
			// Trade
			case 5: if (p_out[selP]) { return false; } // NO BREAK
			// Invade
			case 7: return inRange() &&
					p_explored[selP][selE] &&
					p_owner[selP] != selE &&
					p_owner[selP] != 0 &&
					(a != 7 || e_ships[selE] > 0);
			// Advanced Fuels
			case 9: return e_range[selE] == 5;
			// Terraforming
			case 10: return !e_terraform[selE];
			// Cloak
			case 11: return !e_cloak[selE];
			// Economics
			case 12: return e_econBonus[selE] < 3;
			// Weapons
			case 13: return e_gunBonus[selE] < 3;
			// Scanners
			case 14: return !e_scanner[selE];
			// Transcend, Build Warship, Get Max Income
			case 8: case 15: case 16: return true;
			// Smart invade
			case 17:
				/*int p2 = selP;
				int best = -1;
				for (selP = 0; selP < 24; selP++) {
					if (allowed(7) && (best == -1 || p_money[selP] > p_money[best]) &&
						e_money[selE] + value(7) >= 0)
					{
						best = selP;
					}
				}
				selP = best;
				if (best != -1) {
					return true;
				}
				selP = p2;
				return false;*/
				int p2 = selP;
				int best = -1;
				int bestV = 0;
				for (selP = 0; selP < 24; selP++) {
					if (!allowed(7)) { continue; }
					int v = 300 + p_money[selP] * 3 + value(7) + p_defence[selP] * 20;
					int losses = (p_defence[selP] + (e_f_pos[p_owner[selP]]) == selP ? e_ships[p_owner[selP]] : 0) - e_ships[selE] / 3;
					int newFleet = e_ships[selE];
					int enFleet = e_f_pos[p_owner[selP]] == selP ? e_ships[p_owner[selP]] / 2 : e_ships[p_owner[selP]];
					if (losses > 0) { v -= losses * 80; newFleet -= losses; }
					if (e_f_pos[p_owner[selP]] == selP) { v += e_ships[p_owner[selP]] * 40; }
					// Will it blend / can we hold it?
					if (newFleet + p_defence[selP] / 2 <= enFleet * 4 / 3) {
						v = 0;
					}
					if (v > bestV && e_money[selE] + value(7) >= 0) {
						best = selP;
						bestV = v;
					}
				}
				selP = best;
				if (best != -1) {
					int losses = (p_defence[selP] + (e_f_pos[p_owner[selP]]) == selP ? e_ships[p_owner[selP]] : 0) - e_ships[selE] / 3;
					return true;
				}
				selP = p2;

				return false;
			// Upgrade planetary defences if needed
			default:
				int maxShips = 0;
				for (int e = 1; e < 5; e++) {
					if (e != selE && e_ships[e] > maxShips) { maxShips = e_ships[e]; }
				}
				for (int p = 0; p < 24; p++) {
					if (p_owner[p] == selE && p_defence[p] < maxShips + (a - 20)) {
						int pp = selP;
						selP = p;
						if (e_money[selE] + value(4) >= 0) {
							return true;
						} else {
							selP = pp;
						}
					}
				}
				return false;
		}
	}

	boolean inRange() {
		for (int p2 = 0; p2 < 24; p2++) {
			if (p_owner[p2] == selE &&
					((p_x[p2] - p_x[selP]) * (p_x[p2] - p_x[selP]) + (p_y[p2] - p_y[selP]) * (p_y[p2] - p_y[selP])) <=
					e_range[selE] * e_range[selE])
			{
				return true;
			}
		}
		return false;
	}

	int value(int a) {
		if (a > 19) { a = 4; } // All 20x are improve defences
		switch (a) {
			// Skip turn
			case -1: return 0;
			// Explore
			case 0: return -10;
			// Outpost
			case 1: return -30;
			// Colony
			case 2: return (e_terraform[selE] || p_special[selP] == 1) ? -70 : -140;
			// Get Max Income: This actually selects the correct planet as a side-effect
			case 16:
				int pp = selP;
				int best = -1;
				for (selP = 0; selP < 24; selP++) {
					if (inRange() &&
							(best == -1 ||
							// Prefer raid > tax > trade
							(p_owner[selP] == selE ? 3 : e_cloak[selE] ? 5 : 2) * p_money[selP]
							>
							(p_owner[best] == selE ? 3 : e_cloak[selE] ? 5 : 2) * p_money[best]) &&
							p_owner[selP] != 0)
					{
						best = selP;
					}
				}
				if (best == -1) {
					selP = pp;
					return 0;
				}
				selP = best;
			// Tax, trade, raid
			case 3: case 5: case 6: return p_money[selP];
			// Improve Defences
			case 4: return -60 - 10 * p_defence[selP];
			// Invade
			case 17: case 7: return -50 - Math.max(0,
					(
					p_defence[selP] * 2
					+ (e_f_pos[selE] == selP ? e_ships[p_owner[selP]] * (2 + e_gunBonus[p_owner[selP]]): 0)
					- (e_ships[selE] * (2 + e_gunBonus[selE]))
					)
					* 50);
			// Build Warship
			case 8: return -60;
			// Tech: Econ
			case 12: return new int[] { -400, -1600, -4800, -1 }[e_econBonus[selE]];
			// Tech: Guns
			case 13: return new int[] { -400, -1200, -3600, -1 }[e_gunBonus[selE]];
			// Transcendence Device
			case 15: return -32000;
			// Tech
			default: return -400;
		}
	}

	String[] s_names = {
		"Barren",
		"Fertile",
		"Rich",
		"Poor",
		"Ancient Artefacts",
		"Ancient Warship",
		"Defensible"
	};

	String[] e_names = {
		"Uninhabited",
		"Brown",
		"Red",
		"Blue",
		"Green"
	};

	// Empires have an e prefix
	Color[] e_color = { Color.LIGHT_GRAY, new Color(100, 75, 10), new Color(91, 0, 0), new Color(0, 0, 200), new Color(0, 63, 0) };

	// Planets have a p prefix
	int[] p_x       = new int[24];
	int[] p_y       = new int[24];
	int[] p_special = new int[24];
	int[] p_owner   = new int[24];
	boolean[][] p_explored = new boolean[24][5];
	boolean[] p_out = new boolean[24];
	int[] p_money   = new int[24];
	int[] p_defence = new int[24];

	// Game state
	int selE = 1;
	int selP = 1;

	// Empires
	int[] e_range = { -1, 5, 5, 5, 5 };
	int[] e_ships = { -1, 0, 0, 0, 0 };
	int[] e_money = { -1, 0, 0, 0, 0 };
	int[] e_f_pos = new int[5];

	boolean[] e_lost = new boolean[5];
	boolean[] e_won = new boolean[5];
	int[] e_p_sel = new int[5];
	boolean[] e_human     = { false, true, false, false, false };
	boolean[] e_terraform = { false, false, false, false, true };
	boolean[] e_cloak     = { false, false, false, true, false };
	int[] e_econBonus     = { 0, 1, 0, 0, 0 };
	int[] e_gunBonus      = { 0, 0, 1, 0, 0 };
	boolean[] e_scanner   = new boolean[5];
	boolean[] e_transcend = new boolean[5];
	String[][] e_msg_fromto = {
		{"","","","","",},
		{"","","","","",},
		{"","","","","",},
		{"","","","","",},
		{"","","","","",}
	};

	boolean setup = true;
	boolean antechamber = true;

	boolean needAntechamber() {
		int hps = 0;
		for (int e = 1; e < 5; e++) {
			if (e_human[e]) { hps++; }
		}
		return hps > 1 && e_human[selE];
	}

	public void run() {
		int p;
		for (p = 0; p < 24; p++) {
			search: while (true) {
				int x = r.nextInt(16);
				int y = r.nextInt(16);
				for (int i = 0; i < 24; i++) {
					if (p_x[i] == x && p_y[i] == y) {
						continue search;
					}
				}
				p_x[p] = x;
				p_y[p] = y;
				p_owner[p] = 0;
				p_defence[p] = 0;
				p_special[p] = r.nextInt(7);
				break;
			}
		}
		for (p = 1; p < 5; p++) {
			p_owner[p] = p;
			p_defence[p] = 5;
			p_explored[p][p] = true;
			e_money[p] = 200;
			p_special[p] = 2;
			e_p_sel[p] = p;
			e_f_pos[p] = p;
		}
		e_f_pos[0] = -1;

		game: while (true) {
			try { Thread.sleep(50); } catch (Exception ex) {}

			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

			g.setFont(new Font("Helvetica", 0, 11));

			if (setup) {
				if (click != null) {
					for (int e = 1; e < 5; e++) {
						if (click.getX() > 200 && click.getX() < 380 &&
							click.getY() > 200 + e * 40 && click.getY() < 230 + e * 40)
						{
							e_human[e] = click.getX() < 290;
						}
					}
					if (click.getX() > 100 && click.getX() < 380 &&
						click.getY() > 400 && click.getY() < 430)
					{
						setup = false;
						antechamber = needAntechamber();
					}
				}
				click = null;

				g.setColor(Color.BLACK);
				g.fillRect(0, 0, 712, 600);
				for (int e = 1; e < 5; e++) {
					g.setColor(e_color[e]);
					g.fillRect(100, 200 + e * 40, 80, 30);
					g.fill3DRect(200, 200 + e * 40, 80, 30, !e_human[e]);
					g.fill3DRect(300, 200 + e * 40, 80, 30, e_human[e]);
					g.setColor(Color.GRAY);
					g.fill3DRect(100, 400, 280, 30, true);
					g.setColor(Color.WHITE);
					g.drawString("Start", 110, 415);
					g.drawString(e_names[e], 110, 215 + e * 40);
					g.drawString("Human", 210, 215 + e * 40);
					g.drawString("Computer", 310, 215 + e * 40);
				}
				
				g.drawString("A small space 4X game.", 80, 80);

				g.drawString("Setup:", 80, 230);

				g.setFont(g.getFont().deriveFont(48.0f));
				g.drawString("Moo4k", 80, 50);

				strategy.show();
				continue;
			}

			if (e_lost[selE]) {
				for (p = 0; p < 24; p++) {
					p_explored[p][selE] = true;
				}
				doAction(-1);
				continue;
			}
			if (!e_won[selE]) {
				// User input
				if (antechamber) {
					if (click != null) {
						antechamber = false;
					}
				} else {
					if (e_human[selE]) {
						for (p = 0; p < 9; p++) {
							if (key[a_shortcuts[p]]) {
								doAction(p);
								click = null;
								continue game;
							}
						}
						if (click != null) {
							for (p = 0; p < 24; p++) {
								if (click.getX() / 32 == p_x[p] && click.getY() / 32 == p_y[p]) {
									selP = p;
								}
							}
							if (click.getX() > 512 && click.getY() < 512) {
								doAction(click.getY() / 32);
								click = null;
								continue;
							}
						}
					} else {
						while (!e_human[selE] && !e_won[selE]) {
							ail: for (int a = 0; a < ai.length; a++) {
								for (int pp = 0; pp < 24; pp++) {
									selP = pp;
									if (doAction(ai[a])) { break ail; }
								}
							}
						}
					}
				}
			}
			click = null;

			// Drawing
			g.setColor(e_color[selE]);
			g.fillRect(0, 0, 712, 600);
			g.setColor(Color.WHITE);
			if (e_won[selE]) {
				g.drawString(e_names[selE] + " has won!", 10, 300);
				strategy.show();
				continue;
			}
			if (antechamber) {
				g.drawString(e_names[selE] + ": Click to continue", 10, 300);
				strategy.show();
				continue;
			}

			g.setColor(Color.BLACK);
			g.fillRect(2, 2, 708, 598);

			g.setColor(new Color(30, 30, 40));
			for (p = 0; p < 24; p++) {
				if (selE == p_owner[p]) {
					g.fillOval(
							p_x[p] * 32 - e_range[selE] * 32 + 16,
							p_y[p] * 32 - e_range[selE] * 32 + 16,
							e_range[selE] * 64,
							e_range[selE] * 64);
				}
			}

			for (p = 0; p < 24; p++) {
				g.setColor(Color.WHITE);
				if (p == selP) {
					g.fillOval(p_x[p] * 32 + 4, p_y[p] * 32 + 4, 24, 24);
				}
				g.setColor(p_explored[p][selE] ? e_color[p_owner[p]] : Color.DARK_GRAY);
				g.fillOval(p_x[p] * 32 + 6, p_y[p] * 32 + 6, 20, 20);
				if (p_explored[p][selE]) {
					g.setColor(g.getColor().brighter().brighter());
					g.drawArc(p_x[p] * 32 + 2, p_y[p] * 32 + 2, 28, 28, 0, p_defence[p] * 8);
					g.setColor(Color.LIGHT_GRAY);
					if (e_f_pos[p_owner[p]] == p) {
						g.drawArc(p_x[p] * 32, p_y[p] * 32, 32, 32, 180, e_ships[p_owner[p]] * 8);
					}
					if (p_out[p]) {
						g.fillOval(p_x[p] * 32 + 10, p_y[p] * 32 + 10, 12, 12);
					} else {
						g.setColor(Color.WHITE);
						if (p_owner[p] != 0) {
							g.drawString(p_money[p] + "$", p_x[p] * 32 + 10, p_y[p] * 32 + 18);
						}
					}
				}
			}

			g.setColor(Color.WHITE);
			g.drawString(
					p_explored[selP][selE]
					? n(selP) + ", " + s_names[p_special[selP]] + ", " + p_defence[selP] + " defence" +
						(e_f_pos[p_owner[selP]] == selP ? ", " + e_ships[p_owner[selP]] + " ships" : "")
					: "Unexplored Planet",
					5, 520);
			g.drawString(e_money[selE] + "$, " + e_ships[selE] + " warships at " + n(e_f_pos[selE]), 5, 532);

			// Messagiplex
			for (int i = 1; i < 5; i++) {
				g.setColor(e_color[i]);
				g.fillRect(12, 528 + i * 13, 492, 13);
				g.setColor(Color.WHITE);
				g.drawString(e_msg_fromto[i][selE], 20, 538 + i * 13);
			}

			g.setColor(e_color[selE]);
			g.fillRect(512, 0, 200, 600);
			// Buttons
			for (int a = 0; a < 16; a++) {
				if (allowed(a)) {
					g.setColor(Color.DARK_GRAY);
					g.fill3DRect(513, a * 32 + 1, 199, 30, true);
					g.setColor(e_money[selE] + value(a) < 0 ? Color.GRAY : Color.WHITE);
					g.drawString(a_names[a] + (value(a) >= 0 ? " (+" : " (") + value(a) + "$)", 520, a * 32 + 20);
				}
			}

			strategy.show();
		}
	}

	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		click = e;
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void keyTyped(KeyEvent e) {}

	public void keyPressed(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = true;
	}

	public void keyReleased(KeyEvent e) {
		key[((KeyEvent) e).getKeyCode()] = false;
	}
}
