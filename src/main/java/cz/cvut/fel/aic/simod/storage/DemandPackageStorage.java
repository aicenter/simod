package cz.cvut.fel.aic.simod.storage;

import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.simod.entity.DemandPackage;

@Singleton
public class DemandPackageStorage extends EntityStorage<DemandPackage>
{
	public DemandPackageStorage()
	{
		super();
	}
}
