package hyperheuristic.hh;

/*
    Copyright (c) 2012  Mustafa MISIR, KU Leuven - KAHO Sint-Lieven, Belgium

 	This file is part of GIHH v1.0.

    GIHH is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

public enum HeuristicClassType {

    /**
     * Generic heuristic class types for the generality of hyper-heuristics
     */
    OnlyImproving,
    ImprovingMoreOrEqual,
    WorseningMore,
    OnlyWorsening,
    OnlyEqual

}