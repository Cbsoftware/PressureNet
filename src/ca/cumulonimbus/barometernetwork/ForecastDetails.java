package ca.cumulonimbus.barometernetwork;

import java.util.Random;

/**
 * Represent all useful details of a PressureNet forecast alert to the user
 * @author jacob
 *
 */
public class ForecastDetails {

	// General
	private String timingText = "in 1 hour.";
	
	// Rain
	private String[] rainProbabilityText = {
			"Rain likely to splash", 
			"Droplets expected to sprinkle",
			"You might be singing in the rain",
			"Rain predicted to pour",
			"Rain slated to splash down",
			"Rain anticipated",
			"Rain expected to sprinkle",
			"Showers anticipated overhead"
	};
	private String[] rainHumanText = {
			"Heads up!", 
			"Drip drop!", 
			"Umbrella handy?"
	};

	// Hail
	private String[] hailProbabilityText = {
			"Hail might head your way",
			"Hail estimated",
			"Hard hitting hail estimated",
			"Hail likely to touch down"
	};	
	private String[] hailHumanText = {
			"Take cover!",
			"Look out below!",	
	};
	
	// Snow
	private String[] snowProbabilityText = {
			"Snow might blow your way",
			"Snowfall predicted",
			"Snow expected to glide down",
			"Snowflakes likely to visit you",
			"Snow likely to breeze through",
			"Snowfall suspected",
	};
	private String[] snowHumanText = {
			"Brrr!",
			"Let it snow!"
	};
	
	// Thunderstorm
	private String[] thunderstormProbabilityText = {
			"Thunderstorms likely to occur",
			"Thunderstorms are expected to brew",
			"Get ready for probable thunderstorms",
			"Thunderstorms expected to boom overhead",
			"Thunderstorms expected to rock your region",
			"Thunderstorms predicted to cloud the skies"
	};
	private String[] thunderstormHumanText = {
			"Boom, clap!"
	};
	
	public String composeNotificationText(String weatherEvent) {
		String finalNotificationText = "";
		if(weatherEvent.matches("Rain")) {
			String human = (rainHumanText[new Random().nextInt(rainHumanText.length)]);
			String prob = (rainProbabilityText[new Random().nextInt(rainProbabilityText.length)]);
			String timing = timingText;
			
			finalNotificationText = human + " " + prob + " " + timing;
		} else if (weatherEvent.matches("Hail")) {
			String human = (hailHumanText[new Random().nextInt(hailHumanText.length)]);
			String prob = (hailProbabilityText[new Random().nextInt(hailProbabilityText.length)]);
			String timing = timingText;
			
			finalNotificationText = human + " " + prob + " " + timing;
		} else if (weatherEvent.matches("Snow")) {
			String human = (snowHumanText[new Random().nextInt(snowHumanText.length)]);
			String prob = (snowProbabilityText[new Random().nextInt(snowProbabilityText.length)]);
			String timing = timingText;
			
			finalNotificationText = human + " " + prob + " " + timing;
		} else if (weatherEvent.matches("Thunderstorm")) {
			String human = (thunderstormHumanText[new Random().nextInt(thunderstormHumanText.length)]);
			String prob = (thunderstormProbabilityText[new Random().nextInt(thunderstormProbabilityText.length)]);
			String timing = timingText;
			
			finalNotificationText = human + " " + prob + " " + timing;
		} else {
			finalNotificationText = "";
		}

		
		return finalNotificationText;
	}


	public String getTimingText() {
		return timingText;
	}
	public void setTimingText(String timingText) {
		this.timingText = timingText;
	}
}
