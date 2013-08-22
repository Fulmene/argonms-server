/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.game.character.inventory;

import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.Pet;
import argonms.common.net.external.CommonPackets;
import argonms.game.GameServer;
import argonms.game.character.ExpTables;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GamePackets;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class PetTools {
	private static final byte MAX_PET_LEVEL = 30;

	private static short getInventorySlot(GameCharacter p, Pet pet) {
		for (Map.Entry<Short, InventorySlot> slot : p.getInventory(Inventory.InventoryType.CASH).getAll().entrySet())
			if (slot.getValue() == pet)
				return slot.getKey().shortValue();

		return 0;
	}

	public static boolean gainCloseness(GameCharacter p, byte petSlot, Pet pet, int gain) {
		if (gain == 0)
			return false;

		int newCloseness = (int) pet.getCloseness() + gain;
		newCloseness = checkForLevelUp(p, petSlot, pet, newCloseness);
		newCloseness = checkForLevelDown(p, pet, newCloseness);
		pet.setCloseness((short) newCloseness);
		return true;
	}

	private static int checkForLevelUp(GameCharacter p, byte petSlot, Pet pet, int closeness) {
		if (closeness >= ExpTables.getClosenessForPetLevel(MAX_PET_LEVEL - 1))
			closeness = ExpTables.getClosenessForPetLevel(MAX_PET_LEVEL - 1);
		if (pet.getLevel() >= MAX_PET_LEVEL || closeness < ExpTables.getClosenessForPetLevel(pet.getLevel()))
			return closeness;

		boolean singleLevelOnly = !GameServer.getVariables().doMultiLevel();
		do {
			pet.setLevel((byte) (pet.getLevel() + 1));
			if (singleLevelOnly && pet.getLevel() < MAX_PET_LEVEL && closeness >= ExpTables.getClosenessForPetLevel(pet.getLevel()))
				closeness = ExpTables.getClosenessForPetLevel(pet.getLevel()) - 1;
		} while (pet.getLevel() < MAX_PET_LEVEL && closeness >= ExpTables.getClosenessForPetLevel(pet.getLevel()));

		p.getClient().getSession().send(GamePackets.writeShowPetLevelUp(petSlot));
		p.getMap().sendToAll(GamePackets.writeShowPetLevelUp(p, petSlot), p);

		return closeness;
	}

	private static int checkForLevelDown(GameCharacter p, Pet pet, int closeness) {
		if (closeness <= 0)
			closeness = 0;
		if (pet.getLevel() <= 1 || closeness >= ExpTables.getClosenessForPetLevel(pet.getLevel() - 1))
			return closeness;

		boolean singleLevelOnly = !GameServer.getVariables().doMultiLevel();
		do {
			pet.setLevel((byte) (pet.getLevel() - 1));
			if (singleLevelOnly && pet.getLevel() > 1 && closeness < ExpTables.getClosenessForPetLevel(pet.getLevel() - 1))
				closeness = ExpTables.getClosenessForPetLevel(pet.getLevel() - 1);
		} while (pet.getLevel() > 1 && closeness < ExpTables.getClosenessForPetLevel(pet.getLevel() - 1));

		return closeness;
	}

	public static boolean gainFullness(Pet pet, int gain) {
		byte newFullness = pet.getFullness();
		newFullness += gain;
		if (newFullness > 0) {
			pet.setFullness((byte) Math.min(100, newFullness));
			return false;
		}

		pet.setFullness((byte) 5);
		return false;
	}

	public static void updatePet(GameCharacter p, Pet pet) {
		p.getClient().getSession().send(CommonPackets.writeInventoryUpdatePet(getInventorySlot(p, pet), pet));
	}
}
