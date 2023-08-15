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

import org.l2jmobius.gameserver.model.TradeItem;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Yme
 */
public class TradeOtherAdd extends ServerPacket
{
	private final TradeItem _item;
	
	public TradeOtherAdd(TradeItem item)
	{
		_item = item;
	}
	
	@Override
	public void write()
	{
		ServerPackets.TRADE_OTHER_ADD.writeId(this);
		writeShort(1); // item count
		writeShort(_item.getItem().getType1()); // item type1
		writeInt(_item.getObjectId());
		writeInt(_item.getItem().getDisplayId());
		writeInt(_item.getCount());
		writeShort(_item.getItem().getType2()); // item type2
		writeShort(_item.getCustomType1());
		writeInt(_item.getItem().getBodyPart()); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
		writeShort(_item.getEnchant()); // enchant level
		writeShort(0);
		writeShort(_item.getCustomType2());
	}
}
