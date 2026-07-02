package dev.createrecipehooks.api;

/**
 * Extension point that allows addon authors to register additional hook sources
 * without modifying the library core.
 *
 * <p>Implement this interface and register via
 * {@link CreateRecipeHooks#registerProvider(IHookProvider)} during mod initialisation.
 *
 * <p>Example — a hypothetical addon adding a new recipe machine:
 * <pre>{@code
 * public class MyAddonHooks implements IHookProvider {
 *
 *     @Override
 *     public String getId() { return "myaddon"; }
 *
 *     @Override
 *     public void register(IRegistrar registrar) {
 *         registrar.addListener(ctx -> {
 *             if (ctx.getSource() == RecipeSource.BASIN) {
 *                 // handle basin recipe
 *             }
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p>The {@link #getId()} return value must be unique across all providers.
 * Providers with duplicate ids will be ignored with a warning log.
 */
public interface IHookProvider {

    /**
     * Unique identifier for this provider.
     * Convention: the mod id of the addon, e.g. {@code "create_enchantment_industry"}.
     *
     * @return non-null, non-empty id string
     */
    String getId();

    /**
     * Called once during library initialisation to allow this provider to register
     * listeners via the given {@link IRegistrar}.
     *
     * <p>All types in this method signature belong to the {@code api} package —
     * no internal library classes are exposed here.
     *
     * @param registrar the registration surface; never {@code null}
     */
    void register(IRegistrar registrar);
}
