package net.knightsandkings.knk.paper.utils;

import net.kyori.adventure.text.format.NamedTextColor;

public interface ColorOptions
{

//0
	//Index...............0
	//FalseCommand........1
	//Currency.Colors.....2
	//Coin.Colors.........3
	//Gem.Colors..........4
	//Stats.Colors........5
	//Friend.Colors.......6
	//Kill.Commands.......7

//1 //FalseCommand & ChatFormat
	public NamedTextColor falsecommand = NamedTextColor.RED; //ColorOptions.falsecommand
	public NamedTextColor error = NamedTextColor.RED;
	public NamedTextColor message = NamedTextColor.GRAY;
//2	//Currency Colors
	public NamedTextColor currencycolor = NamedTextColor.GOLD; //ColorOptions.currencycolor
	public NamedTextColor names = NamedTextColor.GREEN; //ColorOptions.names

//3	//Coin Colors
	public NamedTextColor coinStats = NamedTextColor.YELLOW; //ColorOptions.coins

//4	//Gem Colors
	public NamedTextColor gemStats = NamedTextColor.AQUA; //ColorOptions.gems

//5	//Stats Colors
	public NamedTextColor statsformat = NamedTextColor.GOLD; //ColorOptions.statsformat
	public NamedTextColor statsresults = NamedTextColor.GREEN; //ColorOptions.statsoutcome
	public NamedTextColor stats = NamedTextColor.AQUA; //ColorOptions.stats

//6 //Friend Colors
	public NamedTextColor friendformat = NamedTextColor.GOLD; //ColorOptions.friendformat
	public NamedTextColor friendresults= NamedTextColor.GREEN; //ColorOptions.friendoutcome
	public NamedTextColor friend = NamedTextColor.AQUA; //ColorOptions.friend

//7 //Kill Commands
	public NamedTextColor dead = NamedTextColor.RED;

//8 //Title Promotion
	public NamedTextColor promotionformat = NamedTextColor.GOLD; //ColorOptions.promotionformat
	public NamedTextColor promotionresults = NamedTextColor.GREEN; //ColorOptions.promotionoutcome
	public NamedTextColor demotionformat = NamedTextColor.RED;
	public NamedTextColor demotionsubjects = NamedTextColor.DARK_RED;
	public NamedTextColor unlockformat = NamedTextColor.GREEN;
	public NamedTextColor unlocksubjects = NamedTextColor.DARK_GREEN;

//9 //Message formats
	public NamedTextColor messageformat = NamedTextColor.GOLD; //ColorOptions.messageformat
	public NamedTextColor messagesubjects = NamedTextColor.GREEN; //ColorOptions.messagesubjects
	public NamedTextColor messageachievement = NamedTextColor.AQUA; //ColorOptions.messageachievement
//10//Salary format
	public NamedTextColor salaryformat = NamedTextColor.YELLOW; //ColorOptions.salaryformat
	public NamedTextColor salarysubjects = NamedTextColor.AQUA; //ColoeOptions.salarysubjects
//11//Donator Chat formats
//11.1//Default format
	public NamedTextColor defaultformat = NamedTextColor.DARK_GREEN; //ColorOptions.defaultformat
	public NamedTextColor defaultsubjects = NamedTextColor.GREEN; //ColorOptions.defaultsubjects
//11.2//Noble format
//	public NamedTextColor nobleformat = donator.getDonatorColorSecondary(donator.getDonatorID("noble")); //ColorOptions.nobleformat
//	public NamedTextColor noblesubjects = donator.getDonatorColorPrimary(donator.getDonatorID("noble")); //ColorOptions.noblesubjects
////11.3//Royal format
//	public NamedTextColor royalformat = donator.getDonatorColorSecondary(donator.getDonatorID("royal")); //ColorOptions.royalformat
//	public NamedTextColor royalsubjects = donator.getDonatorColorPrimary(donator.getDonatorID("royal")); //ColorOptions.royalsubjects
////11.4//Dragon Blood format
//	public NamedTextColor dbformat = donator.getDonatorColorSecondary(donator.getDonatorID("dragon blood")); //ColorOptions.dbformat
//	public NamedTextColor dbsubjects = donator.getDonatorColorPrimary(donator.getDonatorID("dragon blood")); //ColorOptions.dbsubjects
//11.5//Staff format
	public NamedTextColor staffformat = NamedTextColor.DARK_AQUA;
	public NamedTextColor staffsubjects = NamedTextColor.BLUE;
//11.6//Owner format
	public NamedTextColor ownerformat = NamedTextColor.GOLD;
	public NamedTextColor ownersubjects = NamedTextColor.DARK_PURPLE;
//12//Skills format
	public NamedTextColor skillsformat = NamedTextColor.DARK_RED;
	public NamedTextColor skillsname = NamedTextColor.GOLD;
	public NamedTextColor skillsnotname = NamedTextColor.RED;
	public NamedTextColor specialskillsname = NamedTextColor.DARK_PURPLE;
	public NamedTextColor skillsinfoachieved = NamedTextColor.GREEN;
	public NamedTextColor skillsinfonotachieved = NamedTextColor.GRAY;
//13//SpecialSkills format
	public NamedTextColor Ninja = NamedTextColor.BLACK;
	public NamedTextColor Shotbow = NamedTextColor.GOLD;
	public NamedTextColor Pickpocket = NamedTextColor.DARK_GRAY;
	public NamedTextColor Forger = NamedTextColor.GREEN;
	public NamedTextColor Assassin = NamedTextColor.BLUE;
	public NamedTextColor Juggernaut = NamedTextColor.DARK_AQUA;
	public NamedTextColor Avenger = NamedTextColor.RED;

	public String statsbrackets = ColorOptions.statsformat + "=================================================";
	public String halfstatsbrackets = ColorOptions.statsformat + "=========================";
	public String devidestatsbrackets = ColorOptions.statsformat + "--------------------------------------------------";
	public String halfdevidestatsbrackets = ColorOptions.statsformat + "-------------------------";

	public String rawbrackets = "=================================================";
	public String rawhalfbrackets = "=========================";
	public String rawdevidebrackets = "--------------------------------------------------";
	public String rawhalfdevidebrackets = "-------------------------";
	public NamedTextColor KAKColor = NamedTextColor.BLUE;
	public String KAKFormatshort = NamedTextColor.GRAY + "[" + NamedTextColor.BLUE + "K and K" + NamedTextColor.GRAY + "] ";
	public String KAKFormat = NamedTextColor.GRAY + "[" + ColorOptions.KAKColor + "Knights and Kings" + NamedTextColor.GRAY + "] ";
	public String messageArrow = "► ";
	public String messageArrowReverse = " ◄";
	public String star = "★";

	public NamedTextColor soulbound = NamedTextColor.RED;
	public NamedTextColor ghosted = NamedTextColor.DARK_GRAY;
}
