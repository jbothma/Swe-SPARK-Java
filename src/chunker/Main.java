package chunker;

public class Main
{
	public static void main(String[] args)
	{
//		String test = "Socialstyrelsens/NCUSG@DS vetenskapliga/AQP0SNDS råd/NCNSN@IS\n" +
//				"Vid/SPS behov/NCNSN@IS anlitar/V@IPAS Socialstyrelsen/NCUSN@DS " +
//				"olika/AQP0PN0S experter/NCUPN@IS till/SPS exempel/NCNSN@IS " +
//				"de/DF@0P@S vetenskapliga/AQP0PN0S råden/NCNPN@DS\n" +
//				"Till/SPS vetenskapliga/AQP0PN0S råd/NCNPN@IS på/SPS " +
//				"Socialstyrelsen/NP00N@0S utses/V@IPSS personer/NCUPN@IS " +
//				"med/SPS framstående/AP000N0S skicklighet/NCUSN@IS och/CCS " +
//				"beprövad/AF0USNIS erfarenhet/NCUSN@IS inom/SPS " +
//				"olika/AQP0PN0S vetenskapsgrenar/NCUPN@IS som/PH@000@S " +
//				"är/V@IPAS av/SPS betydelse/NCUSN@IS för/SPS Socialstyrelsens/NCUSG@DS " +
//				"arbete/NCNSN@IS\n" + "Av/SPS integritetsskäl/NCNPN@IS " +
//				"ska/V@IPAS alla/D0@0P@S vetenskapliga/AQP0PN0S råd/NCNPN@IS " +
//				"avge/V@N0AS en/DI@US@S deklaration/NCUSN@IS om/SPS " +
//				"jäv/NCUSN@IS i/SPS varje/DI@0S@S enskilt/AQPNSNIS ärende/NCNSN@IS\n" +
//				"För/SPS att/CIS accepteras/V@N0SS som/CCS vetenskapligt/AQPNSNIS " +
//				"råd/NCNSN@IS krävs/V@IPSS att/CSS man/PI@USS@S tar/V@IPAS " +
//				"del/NCUSN@IS av/SPS bestämmelserna/NCUPN@DS om/SPS jäv/NCUSN@IS\n" +
//				"De/DF@0P@S vetenskapliga/AQP0PN0S råden/NCNPN@DS utses/V@IPSS " +
//				"för/SPS en/DI@US@S period/NCUSN@IS om/SPS tre/MC00N0S år/NCNPN@IS " +
//				"och/CCS är/V@IPAS kopplade/AF00PN0S till/SPS GD-staben/NP00N@0S\n";

//		String test = "Vid/SPS behov/NCNSN@IS anlitar/V@IPAS";

		String test = ""
				+ "Minimizing/NCUSN@IS acts/uo of/uo violence/uo Development/NP00N@0S and/uo implementation/NCUSN@IS of/uo a/uo telephone-linked/uo care/NP00N@0S TLC/NP00N@0S system/NCNSN@IS for/V@IIAS managing/NCUSN@IS impulsivity/NP00N@0S among/NP00N@0S forensic/uo psychiatric/uo outpatients/uo\n"
				+ "Detta/PF@NS0@S är/V@IPAS en/DI@US@S del/NCUSN@IS av/SPS Nationell/AQPUSNIS psykiatrisamordning/NCUSN@IS\n"
				+ "Projekt/NCNSN@IS Minimizing/NCUSN@IS acts/uo of/uo violence/uo Development/NP00N@0S and/uo implementation/NCUSN@IS of/uo a/uo telephone-linked/uo care/NP00N@0S TLC/NP00N@0S system/NCNSN@IS for/V@IIAS managing/NCUSN@IS impulsivity/NP00N@0S among/NP00N@0S forensic/uo psychiatric/uo outpatients/uo Område/NCNSN@IS Rättspsykiatri/NP00N@0S Sökande/genomförare/NP00N@0S Karolinska/NP00N@0S Institutet/NCNSN@DS Sektionen/NCUSN@DS för/SPS rättspsykiatri/NP00N@0S TeleCoach/NP00N@0S TLC/NP00N@0S är/V@IPAS ett/DI@NS@S automatiserat/AF0NSNIS telemediciniskt/AQPNSNIS system/NCNSN@IS som/PH@000@S innebär/V@IPAS att/CSS användaren/NCUSN@DS möter/V@IPAS en/DI@US@S vänlig/AQPUSNIS inspelad/AF0USNIS mänsklig/AQPUSNIS röst/NCUSN@IS\n"
				+ "Användaren/NCUSN@DS ”för/V@IPAS samtal”/NCUSN@IS med/SPS TeleCoach/NCUSN@IS genom/SPS att/CIS trycka/V@N0AS på/SPS telefonens/NCUSG@DS tryckknappar/NCUPN@IS\n"
				+ "TeleCoach-systemet/NCNSN@DS för/SPS våldsdömda/NP00N@0S bygger/V@IPAS på/SPS grundteserna/NCUPN@DS att/CSS personer/NCUPN@IS med/SPS våldsproblematik/NCUSN@IS har/V@IPAS stödbehov/NCNPN@IS som/PH@000@S inte/RG0S möts/V@IPSS av/SPS befintliga/AQP0PN0S vårdsystem/NCNPN@IS samt/CCS att/CSS impulsivitet/NCNSN@DS är/V@IPAS ett/DI@NS@S framträdande/AP000N0S drag/NCNSN@IS som/PH@000@S förenar/V@IPAS våldsdömda/NP00N@0S\n"
				+ "TeleCoach/NP00N@0S erbjuder/V@IPAS dygnetruntstöd/NCNSN@IS via/SPS mobiltelefonen/NCUSN@DS och/CCS påverkar/V@IPAS impulsiviteten/NCUSN@DS direkt/RGPS genom/SPS övningar/NCUPN@IS som/PH@000@S erbjuder/V@IPAS modellinlärning/NCUSN@IS träning/NCUSN@IS i/SPS affektiv/AQPUSNIS reglering/NCUSN@IS och/CCS interpersonell/AQPUSNIS problemlösning/NCUSN@IS\n"
				+ "I/SPS en/DI@US@S pilotstudie/NCUSN@IS med/SPS deltagare/NCUPN@IS från/SPS kriminalvårdens/NCUSG@DS frivård/NCUSN@IS och/CCS rättspsykiatrisk/AQPUSNIS öppenvård/NCUSN@IS testades/V@IISS systemet/NCNSN@DS under/SPS sex/MC00N0S månader/NCUPN@IS\n"
				+ "Huvudsyftet/NCNSN@DS var/V@IIAS att/CIS undersöka/V@N0AS implementeringsmöjligheter/NCUPN@IS inom/SPS frivården/NCUSN@DS och/CCS rättspsykiatrin/NCUSN@DS\n"
				+ "Ladda/V@N0AS ner/pl projektrapport/NCUSN@IS Ladda/V@N0AS ner/pl sammanfattning/NCUSN@IS Relaterat/AF0NSNIS i/SPS Nationell/AQPUSNIS psykiatrisamordning/NCUSN@IS Projekt/NCNSN@IS Område/NCNSN@IS Sökande/genomförare/AQC00N0S Mellanvårdsformer/NCUPN@IS och/CCS stöd/NCNSN@IS i/SPS boende/NCNSN@IS för/SPS psykiskt/RGPS störda/AF00PN0S lagöverträdare/NCUPN@IS Rättspsykiatri/NP00N@0S Värmlands/NP00G@0S läns/NCNSG@IS landsting/NCNSN@IS Örebro/NP00G@0S läns/NCNSG@IS landsting/NCNSN@IS Landstinget/NCNSN@DS i/SPS Östergötland/NP00N@0S och/CCS Region/NP00N@0S Skåne/NP00N@0S Testutbildningar/NCUPN@IS inom/SPS ramen/NCUSN@DS för/SPS grundutbildning/NCUSN@IS rättspsykiatri/NP00N@0S Rättspsykiatri/NP00N@0S Vänersborgs/NP00G@0S kommun/Högskolecentrum/NCNSN@IS i/SPS Vänersborg/NP00N@0S Vårdkedjeprojektet/NCNSN@DS del/NCUSN@IS Utveckling/NCUSN@IS och/CCS implementering/NCUSN@IS av/SPS kvalificerad/AF0USNIS utredning/NCUSN@IS tidigt/RGPS i/SPS\n"

				+ "Rättspsykiatri/NP00N@0S NU-sjukvården/NP00N@0S Norra/NP00N@0S "
				+ "Älvsborgs/NP00G@0S länssjukhus/NCNSN@IS Psykiatriska/AQP0SNDS "
				+ "kliniken/NCUSN@DS Vårdkedjeprojektet/NCNSN@DS del/NCUSN@IS "
				+ "Framtidens/NCUSG@DS vårdkedja/NCUSN@IS en/DI@US@S "
				+ "motorväg/NCUSN@IS med/SPS smidiga/AQP0PN0S av/SPS och/CCS "
				+ "påfarter/NP00N@0S Rättspsykiatri/NP00N@0S "
				+ "NU-sjukvården/NP00N@0S Norra/NP00N@0S Älvsborgs/NP00G@0S "
				+ "länssjukhus/NCNSN@IS Psykiatriska/AQP0SNDS kliniken/NCUSN@DS"
				;
		
		Chunk npc = new Chunk();
		String[] nps = npc.parse_input(test.split("\n"));

		for(String np : nps)
			System.out.println(np);
	}
}
