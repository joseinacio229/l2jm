/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Maktakien
 */
public class StopMoveInVehicle extends ServerPacket
{
	private final int _objectId;
	private final int _boatId;
	private final Location _pos;
	private final int _heading;
	
	public StopMoveInVehicle(Player player, int boatId)
	{
		_objectId = player.getObjectId();
		_boatId = boatId;
		_pos = player.getInVehiclePosition();
		_heading = player.getHeading();
	}
	
	@Override
	public void write()
	{
		ServerPackets.STOP_MOVE_IN_VEHICLE.writeId(this);
		writeInt(_objectId);
		writeInt(_boatId);
		writeInt(_pos.getX());
		writeInt(_pos.getY());
		writeInt(_pos.getZ());
		writeInt(_heading);
	}
}
