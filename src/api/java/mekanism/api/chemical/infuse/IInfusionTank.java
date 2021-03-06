package mekanism.api.chemical.infuse;

import mekanism.api.chemical.IChemicalTank;

/**
 * Convenience extension to make working with generics easier.
 */
public interface IInfusionTank extends IChemicalTank<InfuseType, InfusionStack> {
}