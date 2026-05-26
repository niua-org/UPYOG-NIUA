/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.common.entity.edcr;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FloorUnit extends Measurement {

    private static final long serialVersionUID = 27L;

    private Occupancy occupancy;
    private List<Occupancy> occupancies = new ArrayList<>();
    private List<Measurement> deductions = new ArrayList<>();
    private BigDecimal totalUnitDeduction;
    private List<Balcony> balconies = new ArrayList<>();
    private Integer number;
    private MeasurementWithHeight lightAndVentilation = new MeasurementWithHeight();
    private MeasurementWithHeight laundryOrRecreationalVentilation = new MeasurementWithHeight();
    private Room kitchen;
    private Room bathRoom;
    private Room bathRoomWaterClosets;
    private List<Room> regularRooms = new ArrayList<>();
    private List<Room> acRooms = new ArrayList<>();
    private Room commonRoom;
    private Integer unitNumber;
    private List<Room> nonInhabitationalRooms = new ArrayList<>();
    private List<Door> nonaHabitationalDoors = new ArrayList<>();
    private List<Door> doors = new ArrayList<>();

    public List<Door> getDoors() {
        return doors;
    }

    public void setDoors(List<Door> doors) {
        this.doors = doors;
    }

    public List<Room> getNonInhabitationalRooms() {
        return nonInhabitationalRooms;
    }

    public void setNonInhabitationalRooms(List<Room> nonInhabitationalRooms) {
        this.nonInhabitationalRooms = nonInhabitationalRooms;
    }
    public List<Occupancy> getOccupancies() {
        return occupancies;
    }

    public void setOccupancies(List<Occupancy> occupancies) {
        this.occupancies = occupancies;
    }

    public Occupancy getOccupancy() {
        return occupancy;
    }
    public List<Balcony> getBalconies() {
        return balconies;
    }

    public void setBalconies(List<Balcony> balconies) {
        this.balconies = balconies;
    }
    public Room getKitchen() {
        return kitchen;
    }

    public void setKitchen(Room kitchen) {
        this.kitchen = kitchen;
    }
    public Room getBathRoom() {
        return bathRoom;
    }

    public void setBathRoom(Room bathRoom) {
        this.bathRoom = bathRoom;
    }
    public Room getBathRoomWaterClosets() {
        return bathRoomWaterClosets;}

    public void setBathRoomWaterClosets(Room bathRoomWaterClosets) {
        this.bathRoomWaterClosets = bathRoomWaterClosets;
    }
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public MeasurementWithHeight getLightAndVentilation() {
        return lightAndVentilation;
    }

    public void setLightAndVentilation(MeasurementWithHeight lightAndVentilation) {
        this.lightAndVentilation = lightAndVentilation;
    }

    public MeasurementWithHeight getLaundryOrRecreationalVentilation() {
        return laundryOrRecreationalVentilation;
    }

    public void setLaundryOrRecreationalVentilation(MeasurementWithHeight laundryOrRecreationalVentilation) {
        this.laundryOrRecreationalVentilation = laundryOrRecreationalVentilation;
    }

    public List<Room> getAcRooms() {
        return acRooms;
    }

    public void setAcRooms(List<Room> acRooms) {
        this.acRooms = acRooms;
    }

    public Integer getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(Integer unitNumber) {
        this.unitNumber = unitNumber;
    }

    public void setOccupancy(Occupancy occupancy) {
        this.occupancy = occupancy;
    }

    public BigDecimal getTotalUnitDeduction() {
        return totalUnitDeduction;
    }

    public void setTotalUnitDeduction(BigDecimal totalDeduction) {
        this.totalUnitDeduction = totalDeduction;
    }

    public List<Measurement> getDeductions() {
        return deductions;
    }

    public void setDeductions(List<Measurement> deductions) {
        this.deductions = deductions;
    }

/**
 * @return the regularRooms
 */
public List<Room> getRegularRooms() {
    return regularRooms;
}

public void addRegularRoom(Room regularRoom) {
    this.regularRooms.add(regularRoom);
}

/**
 * @param regularRooms the regularRooms to set
 */
public void setRegularRooms(List<Room> regularRooms) {
    this.regularRooms = regularRooms;
}

public void addAcRoom(Room acRoom) {
    this.acRooms.add(acRoom);
}

public void addNonInhabitationalRooms(Room nonInhabitationalRooms) {
    this.nonInhabitationalRooms.add(nonInhabitationalRooms);
}

public void addDoor(Door door) {
    this.doors.add(door);
}

public List<Door> getNonaHabitationalDoors() {
    return nonaHabitationalDoors;
}

public void setNonaHabitationalDoors(List<Door> nonaHabitationalDoors) {
    this.nonaHabitationalDoors = nonaHabitationalDoors;
}

public void addNonaHabitationalDoors(Door nonaHabitationalDoors) {
    this.nonaHabitationalDoors.add(nonaHabitationalDoors);
}


}
