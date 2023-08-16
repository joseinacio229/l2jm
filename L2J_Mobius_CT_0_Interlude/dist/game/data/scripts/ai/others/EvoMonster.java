package ai.others;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * EvoMonster spawns...when one of the monsters in the keys dies, the next stage monster will spawn, and a final NPC will appear at the end.
 */
public class EvoMonster extends AbstractNpcAI
{
	private static final Map<Integer, Integer> MONSTERSTAGES = new HashMap<>();
	static
	{
		MONSTERSTAGES.put(1, 2);
		MONSTERSTAGES.put(2, 3);
		MONSTERSTAGES.put(3, 4);
		MONSTERSTAGES.put(4, 5);
	}
	
	private EvoMonster()
	{
		addKillId(MONSTERSTAGES.keySet());
	}
	
	@Override
	public String onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int nextStage = MONSTERSTAGES.get(npc.getId());
		if (nextStage <= 5)
		{
			final Attackable newNpc = (Attackable) addSpawn(nextStage, npc);
			newNpc.setRunning();
		}
		
		// Se for o último estágio, gere o NPC final
		if (nextStage == 5)
		{
			Npc finalNpc = addSpawn(50008, npc); // Substitua 9999 pelo ID real do NPC final
			ThreadPool.schedule(finalNpc::deleteMe, 10000); // Desaparece após 10 segundos
		}
		
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new EvoMonster();
	}
}
