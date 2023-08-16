package org.l2jmobius.gameserver.model.actor.ai;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.BotSpellType;
import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.itemhandlers.ItemSkills;
import net.sf.l2j.gameserver.model.Shortcut;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author BAN - L2JDEV
 */
public class Bot
{
	private final Player _actor;
	private static ScheduledFuture<?> _task;
	
	private int LOW_LIFE_PERCENTAGE = 70;
	private int CHANCE_PERCENTAGE = 50;
	
	private int RADIUS = 1200;
	private int HP_POTION_PERCENTAGE = 0;
	private int MP_POTION_PERCENTAGE = 0;
	
	private boolean TOGGLE_NO_BUFF_PROTECTION = false;
	
	private int _usingTime;
	private int _maximumTime = 10800; // time in seconds
	
	public Bot(Player actor)
	{
		_actor = actor;
	}
	
	public void thinking()
	{
		_usingTime++;
		
		// Avoid continue if player is already acting or its unable to act.
		if (_actor.isMoving() || _actor.getCast().isCastingNow() || _actor.isMovementDisabled() || _actor.isOutOfControl() || forceStopSystem())
			return;
		
		calculatePotions();
		
		if (calculateSweep())
			return;
		
		Monster monster = _actor.getTarget() instanceof Monster ? (Monster) _actor.getTarget() : null;
		if (monster != null && !GeoEngine.getInstance().canSeeTarget(_actor, monster))
			monster = null;
		
		L2Skill skill = null;
		if (percentageHpIsLessThan(LOW_LIFE_PERCENTAGE))
			skill = findSkill(BotSpellType.LOW_LIFE);
		
		if (skill == null && Rnd.get(100) < CHANCE_PERCENTAGE) // Check chance skill with 15% success.
			skill = findSkill(BotSpellType.CHANCE);
		
		if (skill == null)
			skill = findSkill(BotSpellType.COMMON);
		
		if (skill != null && skill.getTargetType() == SkillTargetType.CORPSE_MOB && monster != null && monster.isDead())
			doSkill(skill, false);
		else
		{
			monster = selectTarget();
			_actor.setTarget(monster);
		}
		
		if (skill != null && !_actor.isSkillDisabled(skill))
			doSkill(skill, (skill.isHeal() && percentageHpIsLessThan(95)) || skill.getTargetType() == SkillTargetType.SELF);
		else if (haveActionOnShortcut(2))
			_actor.getAI().tryToAttack(monster);
	}
	
	public void start()
	{
		if (_actor.isInsideZone(ZoneId.PEACE))
		{
			_actor.sendMessage("This fuction is not allowed inside peace zone.");
			return;
		}
		
		if (!_actor.isNoble() && _usingTime >= (_maximumTime * 2))
		{
			_actor.sendMessage("The non-premium players have 3 hours limit of using farming per day.");
			return;
		}
		
		if (_task == null)
		{
			if (!_actor.isNoble())
				_usingTime = _actor.getMemos().getInteger("usingBot", 0);
			
			_task = ThreadPool.scheduleAtFixedRate(() -> thinking(), 500, 500);
			_actor.sendMessage("Auto farming just started.");
			_actor.setTeam(_actor.isMageClass() ? TeamType.BLUE : TeamType.RED);
			_actor.broadcastUserInfo();
			// _actor.startAutoPotion(1539);
			// _actor.startAutoPotion(728);
			_actor.setAutoFarm(true);
		}
	}
	
	public void stop()
	{
		if (_task != null)
		{
			if (!_actor.isNoble())
				_actor.getMemos().set("usingBot", _usingTime);
			
			_task.cancel(false);
			_task = null;
			_actor.sendMessage("Auto farming has been stoped.");
			_actor.setTeam(TeamType.NONE);
			_actor.broadcastUserInfo();
			// _actor.stopAutoPotion(1539);
			// _actor.stopAutoPotion(728);
			_actor.setAutoFarm(false);
		}
	}
	
	public static boolean isActive()
	{
		return _task != null;
	}
	
	public void setLowLifePercentage(int value)
	{
		LOW_LIFE_PERCENTAGE = MathUtil.limit(value, 5, 95);
	}
	
	public int getLowLifePercentage()
	{
		return LOW_LIFE_PERCENTAGE;
	}
	
	public void setChancePercentage(int value)
	{
		CHANCE_PERCENTAGE = MathUtil.limit(value, 5, 95);
	}
	
	public int getChancePercentage()
	{
		return CHANCE_PERCENTAGE;
	}
	
	public void setRadius(int value)
	{
		RADIUS = MathUtil.limit(value, 100, 2500);
	}
	
	public int getRadius()
	{
		return RADIUS;
	}
	
	public void setHpPotionPercentage(int value)
	{
		HP_POTION_PERCENTAGE = MathUtil.limit(value, 0, 95);
	}
	
	public int getHpPotionPercentage()
	{
		return HP_POTION_PERCENTAGE;
	}
	
	public void setMpPotionPercentage(int value)
	{
		MP_POTION_PERCENTAGE = MathUtil.limit(value, 0, 95);
	}
	
	public int getMpPotionPercentage()
	{
		return MP_POTION_PERCENTAGE;
	}
	
	public void setNoBuffProtection(boolean val)
	{
		TOGGLE_NO_BUFF_PROTECTION = val;
	}
	
	public boolean isNoBuffProtected()
	{
		return TOGGLE_NO_BUFF_PROTECTION;
	}
	
	private boolean forceStopSystem()
	{
		if (!_actor.isNoble() && _usingTime >= (_maximumTime * 2))
		{
			_actor.sendMessage("The non-premium players have 3 hours limit of using farming per day.");
			stop();
			return true;
		}
		
		final boolean force = isActive() && TOGGLE_NO_BUFF_PROTECTION && _actor.getAllEffects().length <= 8 && _actor.getTarget() == null;
		if (force || _actor.isInsideZone(ZoneId.PEACE))
		{
			_actor.sendMessage("No buff protected has been activated.");
			stop();
		}
		return force;
	}
	
	public Monster selectTarget()
	{
		Monster monster = null;
		for (Monster toTest : _actor.getKnownTypeInRadius(Monster.class, RADIUS))
		{
			if (toTest == null || toTest.isDead() || !GeoEngine.getInstance().canSeeTarget(_actor, toTest) || !toTest.isAttackableWithoutForceBy(_actor))
				continue;
			
			if (!_actor.getCast().isCastingNow() && !_actor.getAttack().isAttackingNow())
				if (!GeoEngine.getInstance().canMoveToTarget(_actor, toTest))
					continue;
				
			// Avoid target raidbosses, grandbosses and chests.
			if (toTest instanceof RaidBoss || toTest instanceof GrandBoss || toTest instanceof Chest || toTest.isRaidRelated())
				continue;
			
			if (monster == null || _actor.distance2D(monster) > _actor.distance2D(toTest))
				monster = toTest;
		}
		return monster;
	}
	
	private boolean isNecessarySkill(L2Skill skill)
	{
		if (skill == null)
			return false;
		
		final WorldObject target = _actor.getTarget();
		if (target instanceof Monster)
		{
			final Monster monster = (Monster) target;
			if (skill.getSkillType() == SkillType.SPOIL && monster.getSpoilState().isSpoiled())
				return false;
			
			List<AbstractEffect> effects = Arrays.stream(monster.getAllEffects()).filter(e -> e.getSkill().isDebuff()).collect(Collectors.toList());
			if (effects != null && !effects.isEmpty() && effects.stream().anyMatch(e -> e.getSkill().getId() == skill.getId()))
				return false;
			
			if (!monster.isDead() && skill.getTargetType() == SkillTargetType.CORPSE_MOB)
				return false;
			
			return true;
		}
		return false;
	}
	
	private void doSkill(L2Skill skill, boolean isSelfSkill)
	{
		final WorldObject target = _actor.getTarget();
		if (skill == null || !(target instanceof Creature))
			return;
		
		if (isNecessarySkill(skill))
			_actor.getAI().tryToCast(isSelfSkill ? _actor : (Creature) target, skill);
	}
	
	private L2Skill findSkill(BotSpellType type)
	{
		L2Skill skill = null;
		try
		{
			for (int id : Arrays.stream(_actor.getShortcutList().getShortcuts()).filter(s -> s.getPage() == 9 && s.getType() == ShortcutType.SKILL && type.getSlots().contains(s.getSlot())).map(Shortcut::getId).collect(Collectors.toList()))
			{
				if (skill != null && !_actor.isSkillDisabled(skill) && !skill.isToggle() && isNecessarySkill(skill))
					continue;
				
				skill = _actor.getSkill(id);
			}
		}
		catch (Exception e)
		{
			return skill;
		}
		return isNecessarySkill(skill) ? skill : null;
	}
	
	private boolean haveActionOnShortcut(int id)
	{
		return Arrays.stream(_actor.getShortcutList().getShortcuts()).anyMatch(shortcut -> shortcut.getPage() == 9 && shortcut.getType() == ShortcutType.ACTION && shortcut.getId() == id);
	}
	
	private boolean percentageHpIsLessThan(double val)
	{
		return (_actor.getStatus().getHp() * 100.0f / _actor.getStatus().getMaxHp()) < val;
	}
	
	private boolean percentageMpIsLessThan(double val)
	{
		return (_actor.getStatus().getMp() * 100.0f / _actor.getStatus().getMaxMp()) < val;
	}
	
	private void forceUseItem(int itemId)
	{
		ItemSkills itemSkills = new ItemSkills();
		
		itemSkills.useItem(_actor, _actor.getInventory().getItemByItemId(itemId), true);
		
	}
	
	public boolean allowedForNonPremium()
	{
		return _usingTime < (_maximumTime * 2);
	}
	
	private boolean calculateSweep()
	{
		final L2Skill sweep = _actor.getSkills().get(42);
		if (sweep == null)
			return false;
		
		final Monster monster = _actor.getKnownTypeInRadius(Monster.class, RADIUS).stream().filter(x -> x.isDead() && x.getSpoilState() != null && x.getSpoilState().getSpoilerId() == _actor.getObjectId()).findFirst().orElse(null);
		if (monster != null)
		{
			_actor.getAI().tryToCast(monster, sweep);
			return true;
		}
		return false;
	}
	
	private void calculatePotions()
	{
		if (HP_POTION_PERCENTAGE != 0 && percentageHpIsLessThan(HP_POTION_PERCENTAGE))
			forceUseItem(1539);
		
		if (MP_POTION_PERCENTAGE != 0 && percentageMpIsLessThan(MP_POTION_PERCENTAGE))
			forceUseItem(728);
	}
	
	public final Player getActor()
	{
		return _actor;
	}
	
	public final void resetTime()
	{
		_usingTime = 0;
		_actor.getMemos().set("usingBot", 0);
	}
	
	public void toggle()
	{
	    if (_task != null)
	        stop();
	    else
	        start();
	}
}