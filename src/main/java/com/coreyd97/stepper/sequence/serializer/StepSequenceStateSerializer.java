package com.coreyd97.stepper.sequence.serializer;

import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.variable.StepVariable;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Vector;

public class StepSequenceStateSerializer implements JsonSerializer<StepSequenceState>, JsonDeserializer<StepSequenceState> {

    @Override
    public StepSequenceState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String title = obj.get("title") != null ? obj.get("title").getAsString() : "Untitled Sequence";
        StepSequenceState stepSequence = new StepSequenceState(title);
        if(obj.has("globals")) {
            //FUNKY BACKWARDS COMPATIBILITY
            List<StepVariable> globalVars =context.deserialize(obj.getAsJsonObject("globals").getAsJsonArray("variables"), new TypeToken<List<StepVariable>>(){}.getType());
            for (StepVariable variable : globalVars) {
                stepSequence.getGlobalVariableManager().addVariable(variable);
            }
        }
        Vector<StepState> steps = context.deserialize(obj.getAsJsonArray("steps"), new TypeToken<Vector<StepState>>(){}.getType());
        for (StepState step : steps) {
            step.setSequenceState(stepSequence);
            stepSequence.addStep(step);
        }
        return stepSequence;
    }

    @Override
    public JsonElement serialize(StepSequenceState src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("title", src.getTitle());
        //Stupid backwards compatibility
        JsonObject globalsObject = new JsonObject();
        globalsObject.add("variables", context.serialize(src.getGlobalVariableManager().getVariables(), new TypeToken<List<StepVariable>>(){}.getType()));
        json.add("globals", globalsObject);
        json.add("steps", context.serialize(src.getSteps(), new TypeToken<Vector<StepState>>(){}.getType()));
        return json;
    }
}
