package dev.createrecipehooks.api;

// Extension point for addons that provide additional hook sources. Implement it and register via CreateRecipeHooks.registerProvider during mod initialisation.
// The id must be unique; duplicates are ignored with a warning.
public interface IHookProvider {

    // Unique id, usually your mod id.
    String getId();

    // Called once at startup, attach your listeners here.
    void register(IRegistrar registrar);
}
