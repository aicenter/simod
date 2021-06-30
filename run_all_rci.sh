##
## Copyright (c) 2021 Czech Technical University in Prague.
##
## This file is part of the SiMoD project.
##
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU Lesser General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU Lesser General Public License for more details.
##
## You should have received a copy of the GNU Lesser General Public License
## along with this program. If not, see <http://www.gnu.org/licenses/>.
##
sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih.log /home/fiedlda1/Amodsim/rci_launchers/ih.sh
sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-sw.log /home/fiedlda1/Amodsim/rci_launchers/ih-sw.sh
sbatch --mem=30G -t 80 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr.log /home/fiedlda1/Amodsim/rci_launchers/ih-nr.sh
sbatch --mem=30G -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr-sw.log /home/fiedlda1/Amodsim/rci_launchers/ih-nr-sw.sh
sbatch --mem=75G -t 1440 -n30 -o /home/fiedlda1/Amodsim/log/vga.log /home/fiedlda1/Amodsim/rci_launchers/vga.sh
sbatch --mem=75G -t 1000 -n30 -o /home/fiedlda1/Amodsim/log/vga-sw.log /home/fiedlda1/Amodsim/rci_launchers/vga-sw.sh
#sbatch --mem=75G -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim.sh
sbatch --mem=75G -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim-sw.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim-sw.sh
sbatch --mem=75G -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim-30ms.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim-30ms.sh
#sbatch --mem=75G -t 1440 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim600ms.log /home/fiedlda1/Amodsim/rci_launchers/vga-lim600ms.sh