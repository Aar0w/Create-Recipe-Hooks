package dev.createrecipehooks.api;

/**
 * Extension point for addons that provide additional hook sources. Implement it and
 * register via CreateRecipeHooks.registerProvider during mod initialisation. The id
 * must be unique; duplicates are ignored with a warning.
 */
public interface IHookProvider {

    /**
     * Unique identifier for this provider.
     * Convention: the mod id of the addon, e.g. "create_enchantment_industry".
     *
     * @return non-null, non-empty id string
     */
    String getId();

    /**
     * Called once during library initialisation to allow this provider to register
     * listeners via the given IRegistrar.
     *
     * All types in this method signature belong to the api package -
     * no internal library classes are exposed here.
     *
     * @param registrar the registration surface; never null
     */
    void register(IRegistrar registrar);
}
